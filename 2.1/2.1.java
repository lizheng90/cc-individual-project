/**
* Cloud Computing 2.1: Using AWS API
* AndrewId: zli1	Name: Zheng Li
*/

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class Scaling {	
	public static String addCenterInstance(AmazonEC2Client ec2) throws IOException, InterruptedException {
		//Create Instance Request
		RunInstancesRequest centerInstancesRequest = new RunInstancesRequest();
		
		//Configure Center Instance Request
		centerInstancesRequest.withImageId("ami-b04106d8")
		.withInstanceType("m3.medium")
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("cc project 1.1")
		.withSecurityGroups("AWS API");		

		RunInstancesResult centerInstancesResult = ec2.runInstances(centerInstancesRequest);
		Instance centerInstance = centerInstancesResult.getReservation().getInstances().get(0);
		CreateTagsRequest centerTagsRequest = new CreateTagsRequest();
		String centerInstanceId = centerInstance.getInstanceId();
		System.out.println(centerInstanceId);
		String centerDnsName = "";
		
		//Check instance state to get public DNS name
		while(true) {	
	    	DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(centerInstanceId);
		    DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
		    InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
	    	if(state.getName().equals("pending")) {
	    		Thread.sleep(10000);
	    	}
	    	else {	
	    		//Tag instance and get center DNS name
	    		centerTagsRequest.withResources(centerInstance.getInstanceId()).withTags(new Tag("Project", "2.1"));
	    		ec2.createTags(centerTagsRequest);
	   
	    	    centerDnsName = describeInstanceResult.getReservations().get(0).getInstances().get(0).getPublicDnsName();
	    		break;
	    	}
	    }
		System.out.println(centerDnsName);
		return centerDnsName;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		//Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(Scaling.class.getResourceAsStream("/AwsCredentials.properties"));
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));

		//Create an Amazon EC2 Client
		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
				
		//Create Instance Request
		RunInstancesRequest loadInstancesRequest = new RunInstancesRequest();
		
		//Configure Load Instance Request
		loadInstancesRequest.withImageId("ami-4c4e0f24")
		.withInstanceType("m3.medium")
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("cc project 1.1")
		.withSecurityGroups("AWS API");
		
		//Launch Instance
		RunInstancesResult loadInstancesResult = ec2.runInstances(loadInstancesRequest); 

		//Get Instance DNS name
		Instance loadInstance = loadInstancesResult.getReservation().getInstances().get(0);
				
		//Add a tag to instance
		CreateTagsRequest loadTagsRequest = new CreateTagsRequest();	

		String loadInstanceId = loadInstance.getInstanceId();
		String loadDnsName = "";

		//Check load generator status
	    while(true) {
	    	DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(loadInstanceId);
	    	Thread.sleep(5000);
		    DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
		    InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
	    	if(state.getName().equals("pending")) {
	    		Thread.sleep(10000);
	    	}
	    	else {
	    		//Tag load generator
	    		loadTagsRequest.withResources(loadInstance.getInstanceId()).withTags(new Tag("Project", "2.1"));
	    		ec2.createTags(loadTagsRequest);
	    		
	    	    loadDnsName = describeInstanceResult.getReservations().get(0).getInstances().get(0).getPublicDnsName();
	    		break;
	    	}
	    }
	    
	    String centerDnsName = addCenterInstance(ec2);
		
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://" + loadDnsName + "/test/horizontal?dns=" + centerDnsName);
        
        String testId;
        try {
        	//Wait for HTTP response
        	Thread.sleep(60000);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            String content = EntityUtils.toString(entity);
            testId = content.split("\\.")[1];
            
            String logUrl = "http://" + loadDnsName + "/log?name=test." + testId + ".log";
            System.out.println(logUrl);
            
    		double sum = 0;
    		int numInstance = 1;
    		//Wait for the 2nd minutes
			Thread.sleep(150000);
			
			List<String> list = new ArrayList<String>();
			list.add(centerDnsName);
    		
    		while(true) {
	        	HttpClient newClient = new DefaultHttpClient();
	        	HttpGet newRequest;
    			
    			Ini ini = new Ini();
    	        ini.load(new URL(logUrl));
    	        
    	        Section sec = null;
    	        for(Section tmp : ini.values()) {
    	        	sec = tmp;
    	        }
    	        for(int i = 0; i < sec.size(); i++) {
    	        	sum += Double.valueOf(sec.get(list.get(i)));
    	        }
    	        if(sum > 4000) {
    	        	System.out.println(numInstance);
    	        	break;
    	        }
    	        else {
    	        	String newCenter = addCenterInstance(ec2);
    	        	numInstance++;
    	        	list.add(newCenter);
    	        	sum = 0;
    	            newRequest = new HttpGet("http://" + loadDnsName + "/test/horizontal/add?dns=" + newCenter);
    	        }
    	        //Wait 2.5 minutes to parse second entries after adding new instance, also wait HTTP response
	        	Thread.sleep(150000);
	            newClient.execute(newRequest);
    		}           
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
