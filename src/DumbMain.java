//put -verbose:gc in VM options in Configurations to print GC data
import java.io.*;
import java.util.*;

public class DumbMain {
    //---STATIC VARIABLES---
    private static String configpath;
    private static String filepath;
    private static String dbURL = null;
    private static String dbUser = null;
    private static String dbPass = null;
    private static boolean isRetrieved = false;
    private static boolean isTagged = false;
    private static int startIndex = 0;
    private static int endIndex = Integer.MAX_VALUE; //arbitrary value
    private static double rhoThreshold = 0.2;
    private static List<String> questionBank = new ArrayList<>();
    private static List<String> answerBank = new ArrayList<>();

    public static void main(String[] args) {

    //---LOCAL OBJECTS AND FIELDS---
        List<Map<String, String>> tagsBank = new ArrayList<>();
        String question, answer;
        FreebaseDBHandler db;
        List<String> IDsList = new ArrayList<>(); //placeholder list for nameAlias2IDs method
        //uses a hash structure to ensure unique tags
        Map<String, String> tags = new HashMap<>(); 
        String spot; //stores a tag's corresponding spot when the tag get removed
        String saveTheLine = null; //stores a line in case we had to add it back.
        Set<String> tagIDs = new HashSet<>();
        List<NTriple> tagTriples = new ArrayList<>();
        List<NTriple> predicateTriples = new ArrayList<>();
        List<NTriple> tagtagTriples = new ArrayList<>();
        List<NTriple> tagMedTriples = new ArrayList<>();
        Map<String, NTriple> mediatorTriples = new HashMap<>();
        Map<String, NTriple> mediatorTagTriples = new HashMap<>();
        NTriple mediatorTriple, mediatorTagTriple;
        List<NTriple> answerTriples = new ArrayList<>();
        Set<String> answerIDs = new HashSet<>();
        //matches are saved uniquely based on subject, predicate, mediatorPredicate, object
        Set<List<String>> matches = new HashSet<>(); 
        Set<String> predicates = new HashSet<>();
        List<String> match = new ArrayList<>();

	//variables for console output
        boolean matched;
        int uniqueMatches = 0 , answers = 0, mediators = 0, haveanswers = 0 , count = 0;
        long startTime = System.currentTimeMillis();
        long previousTime = System.currentTimeMillis();

        PrintWriter writer = null;
        try{
            writer = new PrintWriter("../outputs/output.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("SecurityException: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("UnsupportedEncodingException: " + e.getMessage());
        }
        

        //---Prep FUNCTIONS---
        processArgs(args);
        readConfigFile();

	// database
        db = new FreebaseDBHandler(dbURL, dbUser, dbPass);

        if (isRetrieved) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(filepath));
                String line;
                String[] lineData;
                while ((line = reader.readLine()) != null) { //reads all QA lines from text file
                    saveTheLine = line;
                    lineData = line.split(" \\| ");
                    questionBank.add(lineData[0]);
                    answerBank.add(lineData[1]);
                    if (isTagged) { // has TagMe in the name aka has it's entities tagged
                        for (int i = 1; i < lineData.length/2; i++) {
                            //temporarily uses the tags HashMap
                            tags.put(lineData[i*2], lineData[i*2+1]); 
                        }
                        tagsBank.add(new HashMap<>(tags));
                        tags.clear();
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else { //if the file is retirved aka not algorithm readable text file 
            QARetrieval.parseJSON(filepath); //retrieves QAs from the JSON file
            questionBank = QARetrieval.getQuestions();
            answerBank = QARetrieval.getAnswers();
        }
        if (!isTagged) { //if the file is not tagged srart the service once and for all
            TagMe.setRhoThreshold(rhoThreshold);
            TagMe.startWebClient();
        }

	// boundry check
        if (startIndex < 0) startIndex = 0; //ensures startIndex has a minimum value of 0
        //ensures endIndex cannot be less than startIndex
        if (endIndex < startIndex || endIndex > questionBank.size()) 
            endIndex = questionBank.size(); 

    // loop on questions
        for (int i = startIndex; i < endIndex; i++) {
            question = questionBank.get(i);
            answer = answerBank.get(i);
            // System.out.printf("QUESTION %d. %s (%s)\n", i+1, question, answer);
            System.out.printf("------------------------------QUESTION %d -----------------------------\n", i+1);
	    
	        matched = false;
            //skips the QA pair if Q or A is null
            if (question == null || answer == null) continue; 

            if (isTagged){
                tags.putAll(tagsBank.get(i));
            }
            else { // the file is not tagged find the tagges for each question 1 by 1
                System.out.println("QUERYING TagMe...");
                TagMe.tag(question);
                tags.putAll(TagMe.getTags());
            }

            if (tags.size() != 0) {
                //removes tags that are equivalent to the answer
                spot = tags.remove(answer.toLowerCase().trim()); 
                if (spot != null) { //if a tag was removed, the collected spot is used as the tag
                    tags.put(spot.toLowerCase().trim(), spot.toLowerCase().trim()); //in case the spot is also equivalent to the answer
                    tags.remove(answer.toLowerCase().trim()); 
                }
            }
            if (tags.size() == 0) {
                System.out.println("Skipping because no entities"); //prints an empty line for spacing
                System.out.println();
                continue; //skips the QA pair if there are no tags to use
            }
            System.out.println("TAGS: " + tags);

            //bottom-up
            //prepares all freebase IDs with a name or alias matching the answer
            answerIDs = db.nameAlias2IDs(answer, IDsList, answerIDs);
            if (answerIDs.size() == 0){
                System.out.println("Skipping because answer not in freebase"); //prints an empty line for spacing
                System.out.println();
                continue;
            }

            for (String answerID : answerIDs) {
                answerTriples = db.ID2Triples(answerID, answerTriples);
                if (answerTriples == null) // can this ever happen?
                    continue;
                for (NTriple answerTriple : answerTriples) {
                    // System.out.println(answerTriple.toString());
                    if (db.isIDMediator(answerTriple.getObjectID()))
                        mediatorTriples.put(answerTriple.getObjectID(), answerTriple);
                }

                answerTriples.clear();
            }

            //top-down
            // System.out.println("This many tags "+tags.size());
            for (String tag : tags.keySet()) {
                tagIDs = db.nameAlias2IDs(tag, IDsList, tagIDs);
                // System.out.println("This many tag ID "+tagIDs.size());
                for (String tagID : tagIDs) {
                    tagTriples = db.ID2Triples(tagID, tagTriples);
                    if (tagTriples == null) // can this happen? an ID has no triple associated with it!
                        continue;
                    // System.out.println("This many tag triples "+tagTriples.size());
                    for (NTriple tagTriple : tagTriples) {
                        // if (db.isIDMediator(tagTriple.getObjectID())){ // never happens that two mediator are connected but a normal node might be
                        // // mediatorTriples2.put(tagTriple.getObjectID(), tagTriple);
                        //     mediatorTagTriples.put(tagTriple.getObjectID(), tagTriple);
                        // } // think if we actually want to include these
                        // if (db.isIDMediator(tagID)) // never happens that two mediator are connected
                        // if the object of the tagTriple has an ID matching an answer ID
                        // System.out.println("Tag Triple: "+tagTriple.toString());
                        if (answerIDs.contains(tagTriple.getObjectID())) { 
                            tagTriple.setSubject(tag);
                            tagTriple.setObject(answer);
                            match.add(tagTriple.getSubject());
                            match.add(tagTriple.getPredicate());
                            match.add(null);
                            match.add(tagTriple.getObject());
                            if (!matches.contains(match)) {
                                matched = true;
                                matches.add(match);
                                System.out.printf("MATCHED1: %s | %s\n", tags.get(tag), tagTriple.toString());
								// System.out.printf("The match is : %s \n",match);
								// System.out.printf("The AIDs are : %s \n",String.join(",", answerIDs));
                                // System.out.printf("The ID i s : %s from %d answers\n",tagTriple.getObjectID(),answerIDs.size());
                                System.out.println();
                            }
                        }
                        //if the object of the tagTriple has an ID matching a mediator
                        else if (mediatorTriples.containsKey(tagTriple.getObjectID())) { 
                            tagTriple.setSubject(tag); //no object name for tagTriple because mediator
                            mediatorTriple = mediatorTriples.get(tagTriple.getObjectID()); //no object name for mediatorTriple because mediator
                            mediatorTriple.setSubject(answer); 
                            match.add(tagTriple.getSubject());
                            match.add(tagTriple.getPredicate());
                            match.add(mediatorTriple.getPredicate());
                            match.add(mediatorTriple.getSubject()); // this should be the object (aka the answer), because we stored answerTriple here the subject is the asswer (aka desired object)
                            if (!matches.contains(match)) {
				                matched = true;
                                matches.add(match);
                                // System.out.printf("MATCHED2: %s | %s | %s\n", tags.get(tag), tagTriple.toString(), mediatorTriple.toReverseString());
 								// System.out.printf("The match is : %s \n",match);
								// System.out.printf("The MIDs are : %s \n",String.join(",", mediatorTriples.keySet()));
                                // System.out.printf("The ID is : %s from %d mediators\n",tagTriple.getObjectID(), mediatorTriples.size());
                                System.out.println();
                            }
                        }
                        else { // go deeper into the netwerk in a dumb way.
                            if(matches.size() != 0){
                                continue;
                            }
                            if (tagTriple.getObjectID() == null)
                                continue; 
                            tagtagTriples = db.ID2Triples(tagTriple.getObjectID(), tagtagTriples);
                            if (tagtagTriples == null) // can this happen? an ID has no triple associated with it!
                                continue;
                            System.out.println("This many tagTag Triples "+tagtagTriples.size());
                            if (tagtagTriples.size() > 100000){
                                tagtagTriples.clear();
                                continue;
                            }
                            for (NTriple tagtagTriple : tagtagTriples) { 
                                // System.out.println(tagtagTriple);
                                // if the object of the tagTriple has an ID matching an answer ID
                                if (answerIDs.contains(tagtagTriple.getObjectID())) {
                                    // System.out.println(db.ID2name(tagTriple.getObjectID()));
                                    tagtagTriple.setSubject(db.ID2name(tagTriple.getObjectID()));
                                    // System.out.println(tagtagTriple.getSubject());
                                    tagtagTriple.setObject(answer);
                                    System.out.println("        "+tagtagTriple);
                                    match.add(tagtagTriple.getSubject());
                                    match.add(tagtagTriple.getPredicate());
                                    match.add(null);
                                    match.add(tagtagTriple.getObject());
                                    if (!matches.contains(match)) {
                                        matched = true;
                                        matches.add(match);
                                    }
                                }
                                else if (mediatorTriples.containsKey(tagtagTriple.getObjectID())) {
                                    tagtagTriple.setSubject(db.ID2name(tagTriple.getObjectID()));
                                    mediatorTriple = mediatorTriples.get(tagtagTriple.getObjectID());
                                    mediatorTriple.setSubject(answer);
                                    System.out.println("        "+tagtagTriple);
                                    System.out.println("                "+mediatorTriple); 
                                    match.add(tagtagTriple.getSubject());
                                    match.add(tagtagTriple.getPredicate());
                                    match.add(mediatorTriple.getPredicate());
                                    match.add(mediatorTriple.getSubject());
                                    if (!matches.contains(match)) {
                                        matched = true;
                                        matches.add(match);
                                    }
                                }
                                /*else if (db.isIDMediator(tagtagTriple.getSubjectID())){
                                    // tag is oonnected to a mediator and then another node and then answer
                                    //mediatorTagTriples.put(tagtagTriple.getObjectID(), tagtagTriple);
                                    tagMedTriples = db.ID2Triples(tagtagTriple.getObjectID(), tagMedTriples);
                                    if (tagMedTriples.size() > 50000){
                                        tagMedTriples.clear();
                                        continue;
                                    }
                                    System.out.println("TagMed Triple Size " +tagMedTriples.size());
                                    if (tagMedTriples == null) // can this happen? an ID has no triple associated with it!
                                        continue;
                                    for (NTriple tagMedTriple : tagMedTriples) {
                                        if(matches.size() != 0){
                                            continue;
                                        }
                                        // System.out.println(tagMedTriple);
                                        // if the object of the tagTriple has an ID matching an answer ID
                                        if (answerIDs.contains(tagMedTriple.getObjectID())) {
                                            // System.out.println(db.ID2name(tagTriple.getObjectID()));
                                            tagMedTriple.setSubject(db.ID2name(tagMedTriple.getObjectID()));
                                            // System.out.println(tagtagTriple.getSubject());
                                            tagMedTriple.setObject(answer);
                                            System.out.println("        "+tagtagTriple);
                                            match.add(tagMedTriple.getSubject());
                                            match.add(tagMedTriple.getPredicate());
                                            match.add(null);
                                            match.add(tagMedTriple.getObject());
                                            if (!matches.contains(match)) {
                                                matched = true;
                                                matches.add(match);
                                            }
                                        }
                                    }
                                    tagMedTriples.clear();
                                    // System.out.printf("%d\n", ++count);
                                }*/
                            }
                            tagtagTriples.clear();
                        }
                        match.clear();
                    }
                    tagTriples.clear();
                }
                tagIDs.clear();
            }
            if (matches.size() == 0){
                try{
                    writer.printf("%s | %s", question, answer);
                    for (String tag : tags.keySet()) {
                        writer.write(" | " + tag);
                        writer.write(" | " + tags.get(tag));
                    }
                    writer.println();
                } catch (NullPointerException  e) {
                    System.err.println("NullPointerException: " + e.getMessage());
                }
                System.out.printf("No answer but %d AIDs\n",answerIDs.size());
                System.out.println();
            }
            else{
                haveanswers ++;
            }
            mediatorTriples.clear();
            answerIDs.clear();
            IDsList.clear();
            tags.clear();
            System.gc(); //prompts Java's garbage collector to clean up data structures
            if (matched) uniqueMatches++;
            System.out.printf("PROGRESS: %d MATCHES (%d UNIQUE MATCHES)\nTIME: %.3fs FOR QUESTION AND %.3fs SINCE START\n\n",
                    matches.size(), uniqueMatches, (System.currentTimeMillis() - previousTime)/1000.0, 
                                                    (System.currentTimeMillis() - startTime)/1000.0);
            previousTime = System.currentTimeMillis();
            matches.clear(); //can't be too safe
        }
        // System.out.printf("PROCESSING COMPLETE\nRESULTS: %d MATCHES (%d UNIQUE MATCHES)\n", matches.size(), uniqueMatches);
        writer.close();
        tagsBank.clear();
    }

    private static void processArgs(String[] args) {
        if (args.length == 0 || args.length > 5) {
            System.out.printf("USAGE:\tjava Main [path to Freebase config file] [path to .JSON or .TXT file]\n\t" +
                    "java Main [path to Freebase config file] [path to .JSON or .TXT file] [start index]\n\t" +
                    "java Main [path to Freebase config file] [path to .JSON or .TXT file] [start index] [end index]\n\t" +
                    "java Main [path to Freebase config file] [path to .JSON or .TXT file] [start index] [end index] [rho threshold]\n");
            System.exit(1);
        }
        if (args.length >= 3) {
            startIndex = Integer.parseInt(args[2]);
            if (args.length >= 4) {
                endIndex = Integer.parseInt(args[3]);
                if (args.length == 5)
                    rhoThreshold = Double.parseDouble(args[4]);
            }
        }
        configpath = args[0];
        filepath = args[1];
        if (filepath.contains(".txt")) {
            isRetrieved = true;
            if (filepath.contains("TagMe")) isTagged = true;
        }
    }

    private static void readConfigFile() {
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream(configpath);
            prop.load(input);
            dbURL = prop.getProperty("dbURL");
            dbUser = prop.getProperty("dbUser");
            dbPass = prop.getProperty("dbPass");
            input.close();
            prop.clear();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
