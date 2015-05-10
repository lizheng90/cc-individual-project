package cc.cmu.edu.minisite;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.io.*;
import java.lang.Integer;

import io.undertow.io.Sender;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

// hbase api import
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;

// dynamodb api import
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Map;
import java.util.HashMap;

import org.apache.hadoop.hbase.ZooKeeperConnectionException;

import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;

import java.io.File;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class MiniSite {

    public MiniSite() throws Exception{

    }

    public static void main(String[] args) throws Exception{
        final MiniSite minisite = new MiniSite();

        Undertow.builder()
        .addHttpListener(8080, "0.0.0.0")
        .setHandler(Handlers.path().addPrefixPath("/step1", new RDSHandler())
        	.addPrefixPath("/step2", new HBaseHandler())
        	.addPrefixPath("/step3", new DynamoHandler()))
            .build().start(); 
    }
}

class RDSHandler implements HttpHandler {

    private static final String KEY_UID = "id";
    private static final String KEY_PWD = "pwd";

    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // Get parameters
        Deque<String> id = exchange.getQueryParameters().get(KEY_UID);
        Deque<String> pwd = exchange.getQueryParameters().get(KEY_PWD);

        // Send response back
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; encoding=UTF-8");
        exchange.getResponseSender().send(
            getResponse(id.peekFirst(), pwd.peekFirst()));
    }

    public String getResponse(String id, String pwd) throws IOException, SQLException{
        ObjectMapper mapper = new ObjectMapper();
        // JDBC
        DBUtil util = new DBUtil();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String response = "";
        String sql = "SELECT * FROM user_info WHERE userid = '" + id +
                     "' And userpwd='" + pwd + "';";
        try {
            conn = util.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                response = rs.getString("username");
            }
            else {
                response = "Unauthorized";
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
          if (rs != null)
            try {
              rs.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }
          if (pstmt != null)
            try {
              pstmt.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }
          if (conn != null)
            try {
              conn.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }   
        }

        JSONObject res = new JSONObject();
        res.put("name", response);
        String content = "returnRes("+mapper.writeValueAsString(res)+")";
        
        return content;
    }
}

class DBUtil {
    public Connection getConnection() {
        Connection con = null;
        try {
            String url = "jdbc:mysql://ccrds.cevv1veutscs.us-east-1.rds.amazonaws.com:3306/CC_RDS";
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(url, "zli1", "lz900215");
            return con;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void close(Connection conn) {
        try {
            conn.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class HBaseHandler implements HttpHandler {

      private static final String KEY_UID = "id";

      private static final byte[] INFO_ID = Bytes.toBytes("id");
      private static final byte[] INFO_FRIEND = Bytes.toBytes("friend");

      public void handleRequest(HttpServerExchange exchange) throws Exception {
            // Get parameters
            Deque<String> uid = exchange.getQueryParameters().get(KEY_UID);

            if (uid == null || uid.isEmpty()) {
              return;
            }

            String userId = uid.peekFirst();

            if (userId == null) {
              return;
            }

            // Send response back
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; encoding=UTF-8");
            exchange.getResponseSender().send(
                getResponse(uid.peekFirst()));
      }

      public String getResponse(String uid) throws Exception {

            ObjectMapper mapper = new ObjectMapper();

            // Get response
            HBaseConnect hconn = new HBaseConnect();
            HTable table = hconn.getHTable();

            String rowKey = uid;
            Get get = new Get(Bytes.toBytes(rowKey));

            Result result = null;

            try {
              result = table.get(get);
            } catch (IOException e) {
              System.out.println("EXCEPTION " + e.toString());
            }

            byte[] value = result.getValue(INFO_FRIEND, null);
            String resStr = Bytes.toString(value);

            String[] res = resStr.split(" ");
            ArrayList<String> str = new ArrayList<String>();
            for (String tmp : res) {
              str.add(tmp);
            }

            // sort alphabetically
            java.util.Collections.sort(str);

            JSONObject response = new JSONObject();
            JSONArray friends = new JSONArray();
            for (int i = 0; i < str.size(); i++) {
                JSONObject friend = new JSONObject();
                friend.put("userid", str.get(i));
                friends.add(friend);
            }
            response.put("friends", friends);
            String content = "returnRes("+mapper.writeValueAsString(response)+")";

            return content;
        }
}

class HBaseConnect {

    public HTable getHTable() throws Exception {
        Configuration conf = HBaseConfiguration.create();
        System.out.println(1);
        conf.set("hbase.zookeeper.quorum", "172.31.60.179");
                System.out.println(2);

        HTable table = new HTable(conf, "id");
                System.out.println(3);

        return table;
    }
 }

class DynamoHandler implements HttpHandler {
	
	private static final String KEY_UID = "id";

	public void handleRequest(HttpServerExchange exchange) throws Exception {

        // Get parameters
        Deque<String> id = exchange.getQueryParameters().get(KEY_UID);

        // Send response back
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; encoding=UTF-8");
        exchange.getResponseSender().send(
            getResponse(id.peekFirst()));
    }

    public String getResponse(String id) throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	
    	// get DynamoDB client
    	GetClient get = new GetClient();
    	AmazonDynamoDBClient client = get.getAwsClient();
    	
    	client.setRegion(Region.getRegion(Regions.US_EAST_1));

 		String time = null;
    	String url = null;
    	
    	try {
    		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        	key.put("userid", new AttributeValue().withN(id));

        	GetItemRequest request = new GetItemRequest()
        	    .withTableName("dynamoDBzli1")
        	    .withKey(key);

    		GetItemResult result = client.getItem(request);
    		HashMap<String,AttributeValue> item = (HashMap<String,AttributeValue>)result.getItem();
    		time = item.get("time").getS();
    		url  = item.get("url").getS();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}

    	JSONObject response = new JSONObject();
		response.put("time", time);
		response.put("url", url);
		String content = "returnRes("+mapper.writeValueAsString(response)+")";
		
		return content;
    }
}

class GetClient {
	
	public AmazonDynamoDBClient getAwsClient() throws Exception {

		// Read Credential File
		Properties properties = new Properties();
		System.out.println(2);
		File file = new File("/home/ubuntu/AwsCredentials.properties");
		FileInputStream fs = new FileInputStream(file);
		properties.load(fs);
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(bawsc);

		return client;
	}

}

class AllHandler implements HttpHandler {
	private static final String KEY_UID = "id";
    private static final String KEY_PWD = "pwd";

    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // Get parameters
        Deque<String> id = exchange.getQueryParameters().get(KEY_UID);
        Deque<String> pwd = exchange.getQueryParameters().get(KEY_PWD);

        // Send response back
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; encoding=UTF-8");
        exchange.getResponseSender().send(
            getResponse(id.peekFirst(), pwd.peekFirst()));
    }

    public String getResponse(String id, String pwd) throws Exception {
    	String url = null;
    	Strin time = null;
    	String friendlist = null;
    	ArrayList<String> str = new ArrayList<String>();

    	RDSHandler rds = new RDSHandler();
    	String username = rds.getResponse(id, pwd);

    	if (username == null) {
    		str.add("Unauthorized");
    		str.add(" ");
    	}
    	else {
    		str.add(username);
    		HBaseHandler hb = new HBaseHandler();
    		friendlist = hb.getResponse(username);
    	}

    	ArrayList<String> tmp = new ArrayList<String>();
    	DynamoHandler dh = new DynamoHandler();

    	String[] split = friendlist.split(" ");
    	
    	for (String s : split) {
    		String time = 
    	}

        java.util.Collections.sort(str);


    	JSONObject response = new JSONObject();
		
		JSONArray photos = new JSONArray();
		JSONObject photo1 = new JSONObject();
		photo1.put("url", url1);
		photo1.put("time", time1);
		photo1.put("name", friendname1);
		photos.add(phto1);
		response.put("name", name);
		response.put("photos", photos);
		String content = "returnRes("+mapper.writeValueAsString(response)+")";	
    }
}



