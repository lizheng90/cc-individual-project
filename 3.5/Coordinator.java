import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.sql.Timestamp;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Coordinator extends Verticle {
	  	    
        //Default mode: Strongly consistent. Possible values are "strong" and "causal"
        private static String consistencyType = "strong";

        /**
         * TODO: Set the values of the following variables to the DNS names of your
         * three dataCenter instances
         */
        private static final String dataCenter1 = "ec2-52-4-168-49.compute-1.amazonaws.com";
        private static final String dataCenter2 = "ec2-52-4-186-47.compute-1.amazonaws.com";
        private static final String dataCenter3 = "ec2-52-4-178-233.compute-1.amazonaws.com";

        private static HashMap<String, ArrayBlockingQueue<HttpServerRequest>> keyMap = new HashMap<String, ArrayBlockingQueue<HttpServerRequest>>();
        
        @Override    
        public void start() {    	
                //DO NOT MODIFY THIS
//                KeyValueLib.dataCenters.put(dataCenter1, 1);
//                KeyValueLib.dataCenters.put(dataCenter2, 2);
//                KeyValueLib.dataCenters.put(dataCenter3, 3);
                final RouteMatcher routeMatcher = new RouteMatcher();
                final HttpServer server = vertx.createHttpServer();
                server.setAcceptBacklog(32767);
                server.setUsePooledBuffers(true);
                server.setReceiveBufferSize(4 * 1024);
                routeMatcher.get("/put", new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest req) {
                            MultiMap map = req.params();
                            final String key = map.get("key");
                            final String value = map.get("value");
                            //You may use the following timestamp for ordering requests
                            final String timestamp = new Timestamp(System.currentTimeMillis()
                                                            + TimeZone.getTimeZone("EST").getRawOffset()).toString();
                            
                            // update queue and put in map
                            if(keyMap.containsKey(key)) {
                            	try{ 
                            		ArrayBlockingQueue<HttpServerRequest> tmpQueue = keyMap.get(key);
                            		tmpQueue.put(req);
                                  	keyMap.put(key, tmpQueue);
                            	}
                            	catch(InterruptedException e) {
                            		e.printStackTrace();
                            	}
                              	
                            }
                            else {
                            	try {
                            		ArrayBlockingQueue<HttpServerRequest> tmpQueue = new ArrayBlockingQueue<HttpServerRequest>(100);
                                 	tmpQueue.put(req);
                                 	keyMap.put(key, tmpQueue);
                            	}
                            	catch(InterruptedException e) {
                            		e.printStackTrace();
                            	}
                            
                            }
                            
                            // log                      
                            System.out.println("Put" + " " + key + " " + value + " " + timestamp);
                                                                          
                            Thread t = new Thread(new Runnable() {
                                    public void run() {
                                            //TODO: Write code for PUT operation here.
                                            //Each PUT operation is handled in a different thread.
                                            //Highly recommended that you make use of helper functions.
                                    	try {
	                                    		 synchronized (key) {
	                                              	 ArrayBlockingQueue<HttpServerRequest> tmpQueue = keyMap.get(key);
	                                    			 while(!tmpQueue.peek().equals(req)) {
	                                    				 key.wait();
	                                    			 }
	                                    			 tmpQueue.take();
	                                    			 
//	                                    			 KeyValueLib.PUT(dataCenter1, key, value);
//	    	                        	       	     KeyValueLib.PUT(dataCenter2, key, value);
//	    	                        	       	     KeyValueLib.PUT(dataCenter3, key, value);
	    	                        	       	     
	                                    			 key.notifyAll();

	                                    		 }
	                        	       	    }
                                    	catch(Exception e) {
                                    		 e.printStackTrace();
                                    	}
                                    }
                            });
                            t.start();
                            req.response().end(); //Do not remove this
                    }
            });

            routeMatcher.get("/get", new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest req) {
                            MultiMap map = req.params();
                            final String key = map.get("key");
                            final String loc = map.get("loc");
                            //You may use the following timestamp for ordering requests
                            final String timestamp = new Timestamp(System.currentTimeMillis()
                                                            + TimeZone.getTimeZone("EST").getRawOffset()).toString();
                            
                            // update queue and put in map
                            if(keyMap.containsKey(key)) {
                            	try {
                            		ArrayBlockingQueue<HttpServerRequest> tmpQueue = keyMap.get(key);
	                              	tmpQueue.put(req);
	                              	keyMap.put(key, tmpQueue);
                            	}
                            	catch(InterruptedException e) {
                            		e.printStackTrace();
                            	}
                            }
                            else {
                            	try {
                            		ArrayBlockingQueue<HttpServerRequest> tmpQueue = new ArrayBlockingQueue<HttpServerRequest>(100);
                            		tmpQueue.put(req);
                            		keyMap.put(key, tmpQueue);
                            	}
                            	catch(InterruptedException e) {
                            		e.printStackTrace();
                            	}
                            }
                            

                            Thread t = new Thread(new Runnable() {
                                    public void run() {
                                            //TODO: Write code for GET operation here.
                                            //Each GET operation is handled in a different thread.
                                            //Highly recommended that you make use of helper functions.
                                    	try {	
                                    		
                                    		synchronized (key) {
                                             	 ArrayBlockingQueue<HttpServerRequest> tmpQueue = keyMap.get(key);
	                                   			 while(!tmpQueue.peek().equals(req)) {
	                                   				 key.wait();
	                                   			 }
	                                   			 tmpQueue.take();
//	                                    		 String res = KeyValueLib.GET(dataCenter3, key);
//                                    			 System.out.println("Get output" + " " + res);
//                                    			 req.response().end(res);
                                   			 }
//                                    			String res = KeyValueLib.GET(dataCenter3, key);
//                                    			System.out.println("Get output" + " " + res);
//                                    			req.response().end(res);
                                    		}
		                                catch(Exception e) {
		                                   		e.printStackTrace();
		                                }
                                    }
                            });
                            t.start();
                    }
            });

            routeMatcher.get("/consistency", new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest req) {
                    	keyMap = new HashMap<String, ArrayBlockingQueue<HttpServerRequest>>();
                        MultiMap map = req.params();
                        consistencyType = map.get("consistency");
                        //This endpoint will be used by the auto-grader to set the 
                        //consistency type that your key-value store has to support.
                        //You can initialize/re-initialize the required data structures here
                        req.response().end();
                }
        });

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
                @Override
                public void handle(final HttpServerRequest req) {
                        req.response().putHeader("Content-Type", "text/html");
                        String response = "Not found.";
                        req.response().putHeader("Content-Length",
                                        String.valueOf(response.length()));
                        req.response().end(response);
                        req.response().close();
                }
        });
        server.requestHandler(routeMatcher);
        server.listen(8080);
        }
}