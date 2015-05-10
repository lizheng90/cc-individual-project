/*
 * Name: Zheng Li
 * AndrewID : zli1
 * Cloud Computing 2.3: Using Vertx framework and cache in frontend
 */


/*
 * MSB.java
 * 
 * This is a web-service used by the MSB to get targets' private
 * conversations from the databases. The conversations have been
 * encrypted, but I have heard rumors about the key being a part 
 * of the results retrieved from the database. 
 * 
 * 02/08/15 - I have replicated the database instances to make
 * the web service go faster.
 * 
 * To do (before 02/15/15): My team lead says that I can get a 
 * higher RPS by optimizing the retrieveDetails function. I 
 * stack overflowed "how to optimize retrieveDetails function", 
 * but could not find any helpful results. I need to get it done
 * before 02/15/15 or I will lose my job to that new junior systems
 * architect.
 * 
 * 02/15/15 - :'(
 * 
 * 
 */
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/*
 * Maintain a hashmap to retrieve nodes according to targetID, 
 * Maintain a bidirectional linklist to get and set nodes, least used nodes at the head, most
 * recently used nodes at the tail.
 * Both retrieve, get and set method is O(n)
 */
class Node {
	Node pre, next;
	public String targetID;
	public String detail;
	
	public Node(String targetID, String detail) {
		this.targetID = targetID;
		this.detail = detail;
	}
	
	public String getDetail() {
		return this.detail;
	}
}

class Cache {
	private int capacity;
	private int num;
	private HashMap<String, Node> map;
	private Node first, last;
	
	public Cache(int capacity) {
		this.capacity = capacity;
		num = 0;
		map = new HashMap<String, Node>();
		first = null;
		last = null;
	}
	
	public String get(String targetID) {
		Node node = map.get(targetID);
		if(node == null) {
			return null;
		}
		else if(node != last) {
			if(node == first) {
				first = first.next;
			}
			else {
				node.pre.next = node.next;
			}
			node.next.pre = node.pre;
			last.next = node;
			node.pre = last;
			node.next = null;
			last = node;
		}
		return node.detail;
	}
	
	public void set(String targetID, String detail) {
		Node node = map.get(targetID);
		if(node != null) {
			node.detail = detail;
			if(node != last) {
				if(node == first) {
					first = first.next;
				}
				else {
					node.pre.next = node.next;
				}
				node.next.pre = node.pre;
				last.next = node;
				node.pre = last;
				node.next = null;
				last = node;
			}
		}
		else {
			Node newNode= new Node(targetID, detail);
			if(num >= capacity) {
				map.remove(first.targetID);
				first = first.next;
				if(first != null) {
					first.pre = null;
				}
				else {
					last = null;
				}
				num--;
			}
			if(first == null || last == null) {
				first = newNode;
			}
			else {
				last.next = newNode;
			}
			newNode.pre = last;
			last = newNode;
			map.put(targetID, newNode);
			num++;
		}
	}
	
	public HashMap<String, Node> getMap() {
		return this.map;
	}
	
	public int getNum() {
		return this.num;
	}
}

public class MSB extends Verticle {	
	/*
	 * From the pattern shown in Load Generator console, set the capacity as 300.
	 */
    /* Set up three caches, total 985 records*/
		private Cache cache = new Cache(700);
		private Cache cache1 = new Cache(200);
		private Cache cache2 = new Cache(85);
        private String[] databaseInstances = new String[2];
        /* 
         * init -initializes the variables which store the 
         *           DNS of your database instances
         */
        private void init() throws Exception {
                /* Add the DNS of your database instances here */
                databaseInstances[0] = "ec2-52-1-71-200.compute-1.amazonaws.com";
                databaseInstances[1] = "ec2-54-84-67-31.compute-1.amazonaws.com";
                /* Initiate caches*/
                String response = sendRequest(generateRangeURL(0, 1, 700));
        		int i = 1;
        		for(String split : response.split(";")) {
        			cache.set(String.valueOf(i), split);
        			i++;
        		}
        		String response1 = sendRequest(generateRangeURL(0, 1, 100));
        		i = 1;
        		for(String split : response1.split(";")) {
        			cache1.set(String.valueOf(i), split);
        			i++;
        		}
        		String response2 = sendRequest(generateRangeURL(0, 853583, 853682));
        		i = 1;
        		for(String split : response2.split(";")) {
        			cache1.set(String.valueOf(853582 + i), split);
        			i++;
        		}
         		i = 1;
         		while(i < 86) {
         			String tmpID = String.valueOf(10000 * i);
         			cache2.set(tmpID, sendRequest(generateURL(0, tmpID)));
         			i++;
         		}
        }

        /*
         * checkBackend - verifies that the DCI are running before starting this server
         */
        private boolean checkBackend() {
                try{
                        if(sendRequest(generateURL(0,"1")) == null ||
                        sendRequest(generateURL(1,"1")) == null)
                                return true;
                } catch (Exception ex) {
                        System.out.println("Exception is " + ex);
                        return true;
                }

                return false;
        }

        /*
         * sendRequest
         * Input: URL
         * Action: Send a HTTP GET request for that URL and get the response
         * Returns: The response
         */
        private String sendRequest(String requestUrl) throws Exception {

                URL url = new URL(requestUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(connection.getInputStream(), "UTF-8"));

                String responseCode = Integer.toString(connection.getResponseCode());
                if(responseCode.startsWith("2")){
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                        }
                        in.close();
                        connection.disconnect();
                        return response.toString();
                } else {
                        System.out.println("Unable to connect to "+requestUrl+
                        ". Please check whether the instance is up and also the security group settings");
                        connection.disconnect();
                        return null;
                }
        }
        /*
         * generateURL
         * Input: Instance ID of the Data Center
         *                targetID
         * Returns: URL which can be used to retrieve the target's details
         *                      from the data center instance
         * Additional info: the target's details are cached on backend instance
         */
        private String generateURL(Integer instanceID, String key) {
                return "http://" + databaseInstances[instanceID] + "/target?targetID=" + key;
        }

        /*
         * generateRangeURL
         * Input:       Instance ID of the Data Center
         *                      startRange - starting range (targetID)
         *                      endRange - ending range (targetID)
         * Returns: URL which can be used to retrieve the details of all
         *                      targets in the range from the data center instance
         * Additional info: the details of the last 10,000 targets are cached
         *                                      in the database instance
         *                              
         */
        private String generateRangeURL(Integer instanceID, Integer startRange, Integer endRange) {
                return "http://" + databaseInstances[instanceID] + "/range?start_range="
                                + Integer.toString(startRange) + "&end_range=" + Integer.toString(endRange);
        }

        /* 
         * retrieveDetails - you have to modify this function to achieve a higher RPS value
         * Input: the targetID
         * Returns: The result from querying the database instance
         */
        private String retrieveDetails(String targetID) {
                try{
                	System.err.println("E: " + targetID);
                	if(cache.getMap().containsKey(targetID)) {
                		System.err.println("Hit: " + targetID);
                		return cache.get(targetID);
                	}
                	else if(cache1.getMap().containsKey(targetID)) {
                		System.err.println("Hit: " + targetID);
            			return cache1.get(targetID);
                	}
                	else if(cache2.getMap().containsKey(targetID)) {
                		System.err.println("Hit: " + targetID);
                		return cache2.get(targetID);
                	}
                	else {
            			int idValue = Integer.parseInt(targetID);
                		/*when targetID > 400000, trace 2*/
            			if(idValue > 400000) {
                			System.err.println("					Miss: " + idValue);
                			String response = sendRequest(generateRangeURL(0, idValue-32, idValue+32));
                			int i = 0;              			
                			for(String split : response.split(";")) {
                    			cache.set(String.valueOf(idValue-32+i), split);
                    			i++;
                			}
                    		return sendRequest(generateURL(0, targetID));
                		}
            			/*when targetID < 400000, trace 1*/
                		if(idValue < 400000){
                			System.err.println("					Miss: " + idValue);
                			String response = sendRequest(generateRangeURL(0, idValue-32, idValue+32));
                    		int i = 0;
                    		for(String split : response.split(";")) {
                    			cache.set(String.valueOf(idValue-32+i), split);
                    			i++;
                    		}
                    		return sendRequest(generateURL(0, targetID));	
                		}

                	}
                } catch (Exception ex){
                        System.out.println(ex);
                        return null;
                }
				return null;
        }

        /* 
         * processRequest - calls the retrieveDetails function with the targetID
         */
        private void processRequest(String targetID, HttpServerRequest req) throws NumberFormatException, Exception {
                String result = retrieveDetails(targetID);
                if(result != null)
                        req.response().end(result);
                else
                        req.response().end("No resopnse received");
        }

        /*
         * start - starts the server
         */
        public void start() {
                try {
					init();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
                if(!checkBackend()){
                        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
                                public void handle(HttpServerRequest req) {
                                    String query_type = req.path();               
                                    req.response().headers().set("Content-Type", "text/plain");
                                    if(query_type.equals("/target")){
                                            String key = req.params().get("targetID");
                                     
                                            try {
												processRequest(key,req);
											} catch (Exception e) {
												e.printStackTrace();
											}
                                    }
                                    else {
                                            String key = "1";
                                            try {
												processRequest(key,req);
											} catch (Exception e) {
												e.printStackTrace();
											}
                                    }
                                }
                        }).listen(80);
                } else {
                        System.out.println("Please make sure that both your DCI are up and running");
                        System.exit(0);
                }
        }
}

