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
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class RelevantXKCDServlet extends HttpServlet {
	
    public final static long MAX_RUNTIME_MILLIS = 30000;
	public final static double EBIAS = 1.0; //1.0/20.0;
	public final static double TBIAS = 1.0; //20.0;
    
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		System.out.println("RelevantXKCDServlet DoGet");
		
		String action = req.getParameter("action");
		String query = req.getParameter("query").trim();
		String idx = req.getParameter("idx");
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
			ArrayList<Integer> idxs = findRelevantIdx(query);
			Global.buildURLs();
			System.out.println("Idxs: "+idxs);

			resp.getWriter().println(idxs.get(0)/10000.0+" ");
			resp.getWriter().println(idxs.get(1)+" ");
			for(int i = 2; i < idxs.size(); i++) {
				String url = Global.urls.get(idxs.get(i));
				resp.getWriter().println(+idxs.get(i)+" "+url+" ");
			}
		}
		else if(action.equals("train")) {
			String q = query.replaceAll("[^0-9A-Za-z ]", "");
			String[] split = q.split("\\s+");
			for(int i = 0; i < split.length; i++) {
				split[i] = split[i].toLowerCase();
				split[i] = split[i].trim();
			}
			if(idx.equals("0")) {
				System.out.println("Training towards positive");
				Global.bayes.learn("positive", Arrays.asList(split));
			}
			else if(idx.equals("1")) {
				System.out.println("Training towards negative");
				Global.bayes.learn("negative", Arrays.asList(split));
			}
			else {
				System.out.println("WARNING: incorrect idx = "+idx);
			}
		}
	}
	
	
	public ArrayList<Integer> findRelevantIdx(String query) throws IOException{
		String q = query.replaceAll("[^0-9A-Za-z ]", "");
		String[] split = q.split("\\s+");
		for(int i = 0; i < split.length; i++) {
			split[i] = split[i].toLowerCase();
			split[i] = split[i].trim();
		}
		//System.out.println(Arrays.toString(split));

		//Global.bayes.learn("positive", Arrays.asList(split));

		Classification<String, String> classify = Global.bayes.classify(Arrays.asList(split));
		int chosen = 0;
		double chosenProb = 0.0;
		if(classify == null) {
			//System.out.println("Naive Bayes is not initialized");
		}
		// Positive tends towards transcript
		else if(classify.getCategory().equals("positive")) {
			chosenProb = classify.getProbability();
			System.out.println("Naive Bayes is positive with prob "+chosenProb);
			if(chosenProb > 0.7) {
				// Do nothing
			}
		}
		// Negative tends towards explanation
		else {
			chosenProb = classify.getProbability();
			System.out.println("Naive Bayes is negative with prob "+chosenProb);
			if(chosenProb > 0.6) {
				chosen = 1;
			}
		}
		
		// For each xkcd comic (hard-coded to 1290, FIXME)
		ArrayList<Tuple<Integer, Double>> explainWeights = new ArrayList<Tuple<Integer, Double>>();
		ArrayList<Tuple<Integer, Double>> transcriptWeights = new ArrayList<Tuple<Integer, Double>>();

		Global.buildEGlobalDict();
		Global.buildTGlobalDict();
		
		// Calculate magnitude of query vector
		List<Tuple<String, Integer>> queryVector = new ArrayList<Tuple<String, Integer>>();
		for(int i = 0; i < split.length; i++) {
			int idx = -1;
			for(int j = 0; j < queryVector.size(); j++) {
				if(queryVector.get(j).first.equals(split[i])) {
					idx = j;
					break;
				}
			}
			
			if(idx == -1) {
				queryVector.add(new Tuple<String, Integer>(split[i], 1));
			}
			else {
				queryVector.get(idx).second += 1;
			}
		}
		double queryMagnitude = 0.0;
		for(int i = 0; i < queryVector.size(); i++) {
			queryMagnitude += (double)(Math.pow(queryVector.get(i).second, 2));
		}
		queryMagnitude = Math.sqrt(queryMagnitude);
		
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long startMisses = Global.cacheMisses;
		long endMisses = 0;
		boolean earlyStop = false;
		//for(int i = 1; i < 1290; i++) {
		for(int i = 1289; i > 0; i--) {
			HashMap<Integer, Integer> explain = Global.getDict("explain", i);
			if(explain == null) {
				//System.out.println("Warning, NULL: explain_"+i);
				continue;
			}
			HashMap<Integer, Integer> transcript = Global.getDict("transcript", i);
			if(transcript == null) {
				//System.out.println("Warning, NULL: transcript_"+i);
				continue;
			}
			
			// Calculate magnitudes of explain and transcript vectors
			double explainMagnitude = 0.0, transcriptMagnitude = 0.0;
			for(int value : explain.values()) {
				explainMagnitude += (double)(Math.pow(value, 2));
			}
			explainMagnitude = Math.sqrt(explainMagnitude);
			
			for(int value : transcript.values()) {
				transcriptMagnitude += (double)(Math.pow(value,  2));
			}
			transcriptMagnitude = Math.sqrt(transcriptMagnitude);
			
			Double eWeight = 0.0;
			Double tWeight = 0.0;
			// Open word-><idx, numCnt> table
			for(int j = 0; j < queryVector.size(); j++) {

				Tuple<Integer, Integer> entry = getGlobalIdx(queryVector.get(j).first);
				// Will be null if word is not found in global dictionary
				if(entry != null) {
					//System.out.println(split[j]+" idx: "+entry.first+", cnt: "+entry.second);
					
					if(Global.eGlobalDict.containsKey(entry.first)) {
						double docEFreq = 1.0 + Math.log(Global.eGlobalDict.get(entry.first))/Math.log(2);
						
						if(explain.containsKey(entry.first)) {
							eWeight += ((double)explain.get(entry.first))/docEFreq;
							//System.out.println("Explain word '"+split[j]+"' matched! eWeight += "+explain.get(entry.first)+"/"+docEFreq+" = "+eWeight);
						}
					}
					
					if(Global.tGlobalDict.containsKey(entry.first)) {
						double docTFreq = 1.0 + Math.log(Global.tGlobalDict.get(entry.first))/Math.log(2);

						if(transcript.containsKey(entry.first)) {
							tWeight += ((double)transcript.get(entry.first))/docTFreq;
							//System.out.println("Transcript word '"+split[j]+"' matched! tWeight += "+transcript.get(entry.first)+"/"+docTFreq+" = "+tWeight);
						}
					}
				}
			}
			//System.out.println("Dividing eWeight "+eWeight+"/("+queryMagnitude+"*"+explainMagnitude);
			//System.out.println("Dividing tWeight "+tWeight+"/("+queryMagnitude+"*"+transcriptMagnitude);
			if((queryMagnitude != 0) && (explainMagnitude != 0))
				eWeight = eWeight/(Double)(queryMagnitude * explainMagnitude);
			if((queryMagnitude != 0) && (transcriptMagnitude != 0))
				tWeight = tWeight/(Double)(queryMagnitude * transcriptMagnitude);
			//System.out.println("  eWeight = "+eWeight+", tWeight = "+tWeight);
			
			explainWeights.add(new Tuple<Integer, Double>(i, new Double(eWeight)));
			transcriptWeights.add(new Tuple<Integer, Double>(i, new Double(tWeight)));
			

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
			double w = EBIAS*explainWeights.get(i).second + TBIAS*transcriptWeights.get(i).second;
			totalWeights.add(new Tuple<Integer, Double>(new Integer(explainWeights.get(i).first), new Double(w)));
			//System.out.println(i+": eWeight: "+explainWeights.get(i)+", tWeight: "+transcriptWeights.get(i)+", total: "+w);
		}
		//System.out.println("explain b4 sort: "+explainWeights);
		Collections.sort(explainWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});

		//System.out.println("trans b4 sort: "+transcriptWeights);
		Collections.sort(transcriptWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});
		

		//System.out.println("total b4 sort: "+totalWeights);
		Collections.sort(totalWeights, new Comparator<Tuple<Integer, Double>>(){
		    public int compare(Tuple<Integer, Double> s1, Tuple<Integer, Double> s2) {
		        if(s1.second < s2.second)
		        	return 1;
		        if(s1.second > s2.second)
		        	return -1;
		        return 0;
		    }
		});

		//System.out.println(explainWeights);
		//System.out.println(transcriptWeights);
		System.out.println(totalWeights.subList(0,3));
		//System.out.println("Time: "+(endTime-startTime)+", cache misses: "+(endMisses-startMisses));
		
		UniqueQueue<Integer> comics = new UniqueQueue<Integer>();
		ArrayList<Integer> ret = new ArrayList<Integer>();
		

		// TODO: REMOVE DATABASE SEEDING
		boolean seeded = false;
		List<String> tmp = Arrays.asList(split);
		if(tmp.contains("hackathon") || tmp.contains("hackathons") ||
		   tmp.contains("hacktx") || tmp.contains("mhacks") || tmp.contains("penapps") || tmp.contains("hackmit")) {
			seeded = true;
		}
		
		if(chosen != 0)
			ret.add((int)(chosenProb*10000.0));
		else if(seeded)
			ret.add((int)(Math.min(0.99, 0.15+totalWeights.get(0).second)*10000.0));
		else
			ret.add((int)(totalWeights.get(0).second*10000.0));
		ret.add(chosen); // Second entry represents the one that is chosen (i.e. not following standard order)
		
		if(seeded)
			comics.enq(323);
		
		comics.enq(totalWeights.get(0).first);
		comics.enq(explainWeights.get(0).first);
		comics.enq(transcriptWeights.get(0).first);
		comics.enq(totalWeights.get(1).first);
		comics.enq(explainWeights.get(1).first);
		comics.enq(transcriptWeights.get(1).first);
		comics.enq(totalWeights.get(2).first);
		comics.enq(explainWeights.get(2).first);
		comics.enq(transcriptWeights.get(2).first);
		comics.enq(transcriptWeights.get(3).first);
		comics.enq(transcriptWeights.get(4).first);
		comics.enq(transcriptWeights.get(5).first);
		comics.enq(transcriptWeights.get(6).first);
		
		for(int i = 0; i < 10; i++) {
			if(comics.list.size() != 0)
				ret.add(comics.deq());
		}
		return ret;
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
