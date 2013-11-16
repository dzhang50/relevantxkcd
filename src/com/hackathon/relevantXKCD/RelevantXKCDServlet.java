package com.hackathon.relevantXKCD;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class RelevantXKCDServlet extends HttpServlet {
	
    public final static long MAX_RUNTIME_MILLIS = 60000;
    public final static double TRANS_BIAS = 20.0;
    
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		System.out.println("RelevantXKCDServlet DoGet");
		
		String action = req.getParameter("action");
		String query = req.getParameter("query");
		System.out.println("Action: "+action+", query: "+query);
		
		if(action.equals("rebuild")) {
			System.out.println("Rebuilding");
			UserService userService = UserServiceFactory.getUserService();
			User newUser = userService.getCurrentUser();
			if (newUser == null) {
	            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
			}
			else {
				String user = newUser.getNickname();
				System.out.println("User: "+user);
				Global.clearAllCaches();
				resp.sendRedirect("/index.html");
			}
		} else if(action.equals("xkcd")) {
			System.out.println("Query: "+query);
			int idx = findRelevantIdx(query);
			resp.getWriter().println(idx);
		}
	}
	
	
	public int findRelevantIdx(String query) throws IOException{
		String q = query.replaceAll("[^0-9A-Za-z ]", "");
		String[] split = q.split("\\s+");
		for(int i = 0; i < split.length; i++) {
			split[i] = split[i].toLowerCase();
			split[i] = split[i].trim();
		}
		System.out.println(Arrays.toString(split));
		
		// For each xkcd comic (hard-coded to 1290, FIXME)
		ArrayList<Tuple<Integer, Double>> explainWeights = new ArrayList<Tuple<Integer, Double>>();
		ArrayList<Tuple<Integer, Double>> transcriptWeights = new ArrayList<Tuple<Integer, Double>>();

		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long startMisses = Global.cacheMisses;
		long endMisses = 0;
		boolean earlyStop = false;
		//for(int i = 1; i < 1290; i++) {
		for(int i = 1289; i > 0; i--) {
			HashMap<Integer, Integer> explain = Global.getDict("explain_"+Integer.toString(i));
			if(explain == null) {
				continue;
			}
			HashMap<Integer, Integer> transcript = Global.getDict("transcript_"+Integer.toString(i));
			if(transcript == null) {
				continue;
			}
			HashMap<Integer, Integer> globalExplain = Global.getDict("explainDict");
			HashMap<Integer, Integer> globalTranscript = Global.getDict("transcriptDict");
			
			Double eWeight = 0.0;
			Double tWeight = 0.0;
			// Open word-><idx, numCnt> table
			for(int j = 0; j < split.length; j++) {

				Tuple<Integer, Integer> entry = getGlobalIdx(split[j]);
				// Will be null if word is not found in global dictionary
				if(entry != null) {
					//System.out.println(split[j]+" idx: "+entry.first+", cnt: "+entry.second);
					
					// Deal with explain
					// If contained in word, increment eWeight
					if(explain.containsKey(entry.first)) {
						assert(globalExplain.containsKey(entry.first));
						eWeight += ((double)explain.get(entry.first))/((double)globalExplain.get(entry.first));
						//System.out.println("Word "+split[j]+" matched! eWeight += "+explain.get(entry.first)+"/"+globalExplain.get(entry.first)+" = "+eWeight);
					}

					// Deal with transcript
					// If contained in word, increment tWeight
					if(transcript.containsKey(entry.first)) {
						assert(globalTranscript.containsKey(entry.first));
						tWeight += ((double)transcript.get(entry.first))/((double)globalTranscript.get(entry.first));
						//System.out.println("Word "+split[j]+" matched! tWeight += "+transcript.get(entry.first)+"/"+globalTranscript.get(entry.first)+" = "+tWeight);
					}
				}
			}
			explainWeights.add(new Tuple<Integer, Double>(i, eWeight));
			transcriptWeights.add(new Tuple<Integer, Double>(i, tWeight));
			

			endTime = System.currentTimeMillis();
			//System.out.println("Adding ..."+stock+"... runtime = "+(endTime - startTime));
			if((endTime - startTime) > MAX_RUNTIME_MILLIS) {
				earlyStop = true;
			}
			if(earlyStop) {
				System.out.println("Stopping early at "+i+", "+explainWeights.size()+" completed.");
				break;
			}
		}
		endMisses = Global.cacheMisses;
		ArrayList<Tuple<Integer, Double>> totalWeights = new ArrayList<Tuple<Integer, Double>>();
		for(int i = 0; i < explainWeights.size(); i++) {
			double w = (1.0/TRANS_BIAS)*explainWeights.get(i).second + TRANS_BIAS*transcriptWeights.get(i).second;
			totalWeights.add(new Tuple<Integer, Double>(explainWeights.get(i).first, new Double(w)));
			System.out.println(i+": eWeight: "+explainWeights.get(i)+", tWeight: "+transcriptWeights.get(i)+", total: "+w);
		}

		Collections.sort(explainWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});
		
		Collections.sort(transcriptWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});
		
		
		Collections.sort(totalWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});
		
		System.out.println(totalWeights);
		System.out.println("Total time: "+(endTime-startTime));
		System.out.println("Total cache misses: "+(endMisses-startMisses));
		
		return totalWeights.get(0).first;
	}
	
	public Tuple<Integer, Integer> getGlobalIdx(String word) throws IOException{
        
		if(Global.globalDict == null) {
			Global.globalDict = new HashMap<String, Tuple<Integer, Integer>>();
			
			FileInputStream fin = new FileInputStream("dicts/globalDict");
			BufferedReader fileReader = new BufferedReader (new InputStreamReader(fin));
			String line;
			int idx = 0;
			while((line = fileReader.readLine()) != null) {
				String[] split = line.split(" ");
				String name = split[0];
				int val = Integer.parseInt(split[1]);
				Global.globalDict.put(new String(name), new Tuple<Integer, Integer>(new Integer(idx), new Integer(val)));
				idx++;
			}
			fileReader.close();
		}
		return Global.globalDict.get(word);
	}
}
