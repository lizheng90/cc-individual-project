/*
 Name : Zheng Li
 AndrewId : zli1
 Cloud computing project 2.2 : ELB and auto scaling
 */

package autoscaling;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.Statistic;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckResult;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;

public class autoscaling {
	public static void main(String[] args) throws IOException, InterruptedException {
		//Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(autoscaling.class.getResourceAsStream("/AwsCredentials.properties"));
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));

		//Create an Amazon EC2 Client
		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
		
		//Initialize createSecurityGroupRequest
		CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
		csgr.withGroupName("Project 2.2").withDescription("Auto scaling");
		
		//Initialize createSecurityGroupResult
		CreateSecurityGroupResult createSecurityGroupResult = ec2.createSecurityGroup(csgr);
	
		//Initialize ip permission
		IpPermission ipPermission = new IpPermission();
		ipPermission.withIpRanges("0.0.0.0/0") 
					.withIpProtocol("-1")
					.withFromPort(0)
					.withToPort(65536);
		
		//Initialize authorizeSecurityGroupIngressRequest
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = 
				new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupName("Project 2.2").withIpPermissions(ipPermission);
		//AuthorizeSecurityGroupIngress
		ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
				
		//Create load balancer client
		AmazonElasticLoadBalancingClient elb = new AmazonElasticLoadBalancingClient(bawsc);
				
		//Create load balancer
		List<String> SGs = new ArrayList<String>(1);
		List<Listener> listeners = new ArrayList<Listener>(1);
		List<String> availZones = new ArrayList<String>();
		availZones.add("us-east-1e");

		SGs.add(createSecurityGroupResult.getGroupId());
		listeners.add(new Listener("HTTP", 80, 80));
		CreateLoadBalancerRequest clbr = new CreateLoadBalancerRequest()
							.withAvailabilityZones(availZones)
							.withListeners(listeners)
							.withSecurityGroups(SGs)
							.withLoadBalancerName("autoscaling");
		CreateLoadBalancerResult lbResult = elb.createLoadBalancer(clbr);

		//Create health check
		HealthCheck healthCK = new HealthCheck()
		.withInterval(30)
		.withTimeout(5)
		.withHealthyThreshold(2)
		.withUnhealthyThreshold(5)
		.withTarget("HTTP:80/heartbeat");
		ConfigureHealthCheckRequest healthCheckRequest = new ConfigureHealthCheckRequest()
									.withHealthCheck(healthCK)
									.withLoadBalancerName("autoscaling");
		
		ConfigureHealthCheckResult checkResult = elb.configureHealthCheck(healthCheckRequest);
		
		//Wait 1 min to manully change health check and add tag
		Thread.sleep(20000);
		String elbDnsName = lbResult.getDNSName();
		System.out.println(elbDnsName);
		
		//Creat auto scale client and cloudwatch client
		AmazonAutoScalingClient autoScale = new AmazonAutoScalingClient();
		AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient();
		
		//Create Auto Launch Configuration
		CreateLaunchConfigurationRequest lcRequest = new CreateLaunchConfigurationRequest();
		lcRequest.setLaunchConfigurationName("auto 2.2");
		lcRequest.setImageId("ami-7c0a4614");
		lcRequest.setInstanceType("m3.medium"); 
		lcRequest.setKeyName("cc project 1.1");
		
		//Set Security Group
		List<String> SG1 = new ArrayList<String>(1);
		SG1.add(createSecurityGroupResult.getGroupId());
		lcRequest.setSecurityGroups(SG1);
		
		//Set instance monitor
		InstanceMonitoring monitoring = new InstanceMonitoring();
		monitoring.setEnabled(Boolean.TRUE);
		lcRequest.setInstanceMonitoring(monitoring);
		
		//CreateLaunchConfiguration

		autoScale.createLaunchConfiguration(lcRequest);
		Thread.sleep(20000);
		
		
		//Create auto scaling group request
		CreateAutoScalingGroupRequest asgRequest = new CreateAutoScalingGroupRequest();
		asgRequest.setAutoScalingGroupName("asg 2.2");
		asgRequest.setLaunchConfigurationName("auto 2.2");
		asgRequest.setAvailabilityZones(availZones);
		asgRequest.setMinSize(3);
		asgRequest.setMaxSize(5);
		List<String> elbs = new ArrayList<String>(1);
		elbs.add(clbr.getLoadBalancerName());
		asgRequest.setLoadBalancerNames(elbs);
		asgRequest.setHealthCheckType("ELB");
		asgRequest.setHealthCheckGracePeriod(300);//
		
		//Set cool down time
		asgRequest.setDefaultCooldown(180);
		
		//Create asg
		autoScale.createAutoScalingGroup(asgRequest);
		Thread.sleep(20000);
		
		//Create Instance Request
		RunInstancesRequest loadInstancesRequest = new RunInstancesRequest();
		
		//Configure Load Instance Request
		loadInstancesRequest.withImageId("ami-ae0a46c6")
		.withInstanceType("m3.medium")
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("cc project 1.1")
		.withSecurityGroups("AWS API");
		
		//Launch Instance
		RunInstancesResult loadInstancesResult = ec2.runInstances(loadInstancesRequest); 

		//Get loadInstance
		Instance loadInstance = loadInstancesResult.getReservation().getInstances().get(0);
				
		//Create add tag request
		CreateTagsRequest loadTagsRequest = new CreateTagsRequest();	
		
		//Get load instance id and initiate load dns name
		String loadInstanceId = loadInstance.getInstanceId();
		String loadDnsName = "";
		Tag tag = new Tag("Project", "2.2");

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
	    		loadTagsRequest.withResources(loadInstance.getInstanceId()).withTags(tag);
	    		ec2.createTags(loadTagsRequest);
	    		//Get load DNS name
	    	    loadDnsName = describeInstanceResult.getReservations().get(0).getInstances().get(0).getPublicDnsName();
	    		System.out.println(loadDnsName);
	    	    break;
	    	}
	    }
	    
	    Thread.sleep(60000);
		
        String testId;

		//Wait datacenter
	    while(true) {
	    	HttpClient client = new DefaultHttpClient();
	        HttpGet request = new HttpGet("http://" + loadDnsName + "/warmup?dns=" + elbDnsName);
	        System.out.println("http://" + loadDnsName + "/warmup?dns=" + elbDnsName);
	        try {
	        	//Wait for HTTP response
	        	Thread.sleep(60000);
	            HttpResponse response = client.execute(request);
	            HttpEntity entity = response.getEntity();

	            String content = EntityUtils.toString(entity);
	            testId = content.split("\\.")[1];
	            if(!testId.equals(" <br>Valid format = XXXX")) {
	            	System.out.println(testId);
	            	break;
	            }
	        } catch(IOException e) {
	        	e.printStackTrace();
	        }
	    }
			
		//Warm up the elb
	    warmUp(loadDnsName, elbDnsName, testId);
	    warmUp(loadDnsName, elbDnsName, testId);
	     
	    //Start junior test
	    junior(loadDnsName, elbDnsName);
	    
	    //Sleep 4 mins
	    Thread.sleep(240000);
	    
		//Create scaling up policy and alarm
		PutScalingPolicyRequest policyRequest = new PutScalingPolicyRequest();
		policyRequest.setAutoScalingGroupName("asg 2.2");
		policyRequest.setPolicyName("asp 2.2 up");
		
		//Scale up by two
		policyRequest.setScalingAdjustment(2);
		policyRequest.setAdjustmentType("ChangeInCapacity");
		
		//Create scaling policy result
		PutScalingPolicyResult policyResult = autoScale.putScalingPolicy(policyRequest);
		String arn = policyResult.getPolicyARN();
		
		//Set add alarm
		PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
		upRequest.setAlarmName("add instance");
		upRequest.setMetricName("CPUUtilization");
		
		List<Dimension> dimensions = new ArrayList<Dimension>();
		Dimension dimension = new Dimension();
		dimension.setName("AutoScalingGroupName");
		dimension.setValue("asg 2.2");
		dimensions.add(dimension);
		upRequest.setDimensions(dimensions);
		upRequest.setNamespace("AWS/EC2");
		upRequest.setComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold);
		upRequest.setStatistic(Statistic.Average);
		upRequest.setUnit(StandardUnit.Percent);
		upRequest.setThreshold(90d);
		upRequest.setPeriod(60);
		upRequest.setEvaluationPeriods(3);
		
		List<String> actions = new ArrayList<String>();
		actions.add(arn);
		upRequest.setAlarmActions(actions);
		
		cloudWatchClient.putMetricAlarm(upRequest);
		
		//Create scaling down policy and alarm
		PutScalingPolicyRequest policyRequest1 = new PutScalingPolicyRequest();
		policyRequest1.setAutoScalingGroupName("asg 2.2");
		policyRequest1.setPolicyName("asp 2.2 down");
		
		//Scale down by two
		policyRequest1.setScalingAdjustment(-2);
		policyRequest1.setAdjustmentType("ChangeInCapacity");
		
		//Create scaling policy result
		PutScalingPolicyResult policyResult1 = autoScale.putScalingPolicy(policyRequest1);
		String arn1 = policyResult1.getPolicyARN();
		
		//Set decrease alarm
		PutMetricAlarmRequest downRequest = new PutMetricAlarmRequest();
		downRequest.setAlarmName("decrease instance");
		downRequest.setMetricName("CPUUtilization");
		
		List<Dimension> dimensions1 = new ArrayList<Dimension>();
		Dimension dimension1 = new Dimension();
		dimension1.setName("AutoScalingGroupName");
		dimension1.setValue("asg 2.2");
		dimensions1.add(dimension1);
		downRequest.setDimensions(dimensions1);
		downRequest.setNamespace("AWS/EC2");
		downRequest.setComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold);
		downRequest.setStatistic(Statistic.Average);
		downRequest.setUnit(StandardUnit.Percent);
		downRequest.setThreshold(70d);
		downRequest.setPeriod(60);
		downRequest.setEvaluationPeriods(2);
		
		List<String> actions1 = new ArrayList<String>();
		actions1.add(arn1);
		downRequest.setAlarmActions(actions1);
		
		cloudWatchClient.putMetricAlarm(downRequest);
		
		//Sleep 40min
		Thread.sleep(60000 * 48);
		
		//terminate all devices
		
		//terminate security group
		DeleteSecurityGroupRequest deleteSGRequest = new DeleteSecurityGroupRequest()
					.withGroupId(createSecurityGroupResult.getGroupId());
		ec2.deleteSecurityGroup(deleteSGRequest);
		
		//terminate load balancer
		DeleteLoadBalancerRequest deleteLBRequest = new DeleteLoadBalancerRequest()
					.withLoadBalancerName("autoscaling");
		elb.deleteLoadBalancer(deleteLBRequest);
		
		//terminate auto scaling group
		DeleteAutoScalingGroupRequest deleteASGRequest = new DeleteAutoScalingGroupRequest()
					.withAutoScalingGroupName("asg 2.2");
		autoScale.deleteAutoScalingGroup(deleteASGRequest);
		
		//terminate launch configuration
		DeleteLaunchConfigurationRequest deleteALCRequest = new DeleteLaunchConfigurationRequest()
					.withLaunchConfigurationName("autoScaling");
		autoScale.deleteLaunchConfiguration(deleteALCRequest);
		
		//terminate cloud alarms
		List<String> list = new ArrayList<String>();
		list.add(upRequest.getAlarmName());
		list.add(downRequest.getAlarmName());
		DeleteAlarmsRequest deleteAlarms = new DeleteAlarmsRequest()
					.withAlarmNames(list);	
		cloudWatchClient.deleteAlarms(deleteAlarms);
	}
	
	
	//Ping junior
	public static void junior(String loadDnsName, String elbDnsName) throws InterruptedException {
		HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://" + loadDnsName + "/junior?dns=" + elbDnsName);
        
        try {
        	//Wait for HTTP response
        	Thread.sleep(10000);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();		
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	//Ping warmup
    public static void warmUp(String loadDnsName, String elbDnsName, String testId) throws InterruptedException {                
		HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://" + loadDnsName + "/warmup?dns=" + elbDnsName);
        
        try {
        	//Wait for HTTP response
        	Thread.sleep(10000);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();		
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Thread.sleep(60000 * 5);
    }
}




