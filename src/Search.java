import java.io.*;
import java.util.*;

public class Search {
    //---STATIC VARIABLES---
    // This is for every single question. 
    private String answer;
    private FreebaseDBHandler db;
    private Commons cm;
    private Predicates preds;
    private boolean exists, matched = false, check, foundText = false, contained = false;
    private int sizeLimit = 100000 , sizeLimit2 = 50000;

    private Set<String> answerIDs = new HashSet<>();
    private List<String> IDsList = new ArrayList<>(); //placeholder list for nameAlias2IDs method
    private Map<String, NTriple> mediatorTriples = new HashMap<>();

    // Store the names in a seperate table, just so we don't have to search again for repeated ID's
    private Map<String, String> objectIDnames = new HashMap<>();
    private Map<String, String> tags = new HashMap<>(); 
    private Set<String> tagIDs = new HashSet<>();
    private List<NTriple> tagTriples = new ArrayList<>();
    private List<NTriple> comTagTriples = new ArrayList<>();
    // The other tag triples which are connected to strings not objects
    private List<NTriple> otherTagTriples = new ArrayList<>();

    private NTriple mediatorTriple;
    //matches are saved uniquely based on subject, predicate, mediatorPredicate, object
    private Set<List<String>> matches = new HashSet<>();
    private Set<NTriple> secondMatches = new HashSet<>();  
    private List<String> match = new ArrayList<>();

    // private Set<String> objectTypes = new HashSet<>();

    private Scanner console = new Scanner(System.in);
    private String input = new String();

    private Set<String> sortablePreds = new HashSet<>();

    // private Map<String, List<NTriple>> secondTagNames = new HashMap<>(); // Store the triples that 
    // private Map<String, List<NTriple>> goodSecondTriples = new HashMap<>(); // ObjectName and Triple
    private Map<String, List<NTriple>> commonPredicates = new HashMap<>(); // Predicate --> Triples
    private List<NTriple> commonTagTriples = new ArrayList<>();
    private Set<String> commonPreds = new HashSet<>();
    private Map<String, Map<String,Map<String,List<NTriple>>>> allSortable = new HashMap<>();

    public Search(String answer, FreebaseDBHandler db, Map<String, String> tags) {
        this.answer = answer;
        this.db = db;
        this.matched = false;
        this.tags = tags;
        this.mediatorTriples = mediatorTriples;
        this.answerIDs = answerIDs;
        this.exists = false;
        this.check = false;
    }


    public void bottomUp(){
        //prepares all freebase IDs with a name or alias matching the answer
        List<NTriple> answerTriples = new ArrayList<>();
        this.answerIDs = this.db.nameAlias2IDs(this.answer, this.IDsList, this.answerIDs);
        if (this.answerIDs.size() == 0){
            System.out.println("Skipping because answer not in freebase"); //prints an empty line for spacing
            System.out.println();
            this.exists = false; // skip to the next question
            return;
        }
        this.exists = true;

        for (String answerID : this.answerIDs) {
            answerTriples = this.db.ID2Triples(answerID, answerTriples);
            if (answerTriples == null) // can this ever happen?
                continue;
            for (NTriple answerTriple : answerTriples) {
                if (this.db.isIDMediator(answerTriple.getObjectID()))
                    this.mediatorTriples.put(answerTriple.getObjectID(), answerTriple);
            }
            answerTriples.clear();
        }
    }

	public void commonTags(){
        cm = new Commons(db, tags);
        cm.commonPredicatesSet(this.commonPreds);
        Map<String, String> goodTagIDs = new HashMap<>();
        cm.goodIDs(goodTagIDs);
        for (String tagID : goodTagIDs.keySet()) {
            this.comTagTriples = this.db.ID2Triples(tagID, this.comTagTriples);
            if (this.comTagTriples == null) // can this happen? an ID has no triple associated with it!
                continue;
            for (NTriple tagTriple : this.comTagTriples) {
                if(this.commonPreds.contains(tagTriple.getPredicate())){
                    check = false;
                    check = isConnectedToAnswer(goodTagIDs.get(tagID), tagTriple);
                    if (!check) 
                        check = isConnectedToAnswerMediator(goodTagIDs.get(tagID), tagTriple);
                }
            }
            this.comTagTriples.clear();
        }
        for (String tagID : goodTagIDs.keySet()) {
            if(this.matched)
                continue;
            this.comTagTriples = this.db.ID2Triples(tagID, this.comTagTriples);
            if (this.comTagTriples == null) // can this happen? an ID has no triple associated with it!
                continue;
            for (NTriple tagTriple : this.comTagTriples) {
                if(this.matched)
                    continue;
                topDownDeeper(tagTriple.getObjectID(),tagTriple);
            }
            this.comTagTriples.clear();
        }
        this.commonPreds.clear();
        goodTagIDs.clear();
        cm.cleanUp();

        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("                No answers were found using the Common Tags Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
	}

    public void sortablePredicates(){
        int count = 0;
            for (String tag : this.tags.keySet()) {
                this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
                for (String tagID : this.tagIDs) {
                    allSortable.put((tag + ": " + tagID),sortedPredicates(tag, tagID));
                    this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
                    if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
                        continue;
                    for (NTriple tagTriple : this.tagTriples) {
                        if(this.sortablePreds.contains(tagTriple.getPredicate())){
                            check = false;
                            check = isConnectedToAnswer(tag, tagTriple);
                            if (!check) 
                                check = isConnectedToAnswerMediator(tag, tagTriple);
                        }
                    }
                    this.tagTriples.clear();
                }
                this.tagIDs.clear();
            }

        if(!this.matched){
            this.sortablePreds.clear();
            for (String tag : this.tags.keySet()) {
                this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
                // this.IDsList.clear();
                // tagIDValues = this.db.nameAlias2IDs(this.tags.get(tag), this.IDsList, tagIDValues);
                // this.tagIDs.addAll(tagIDValues); // Add also the values of tags!
                for (String tagID : this.tagIDs) {
                    this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
                    if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
                        continue;
                    for (NTriple tagTriple : this.tagTriples) {
                        if(this.sortablePreds.contains(tagTriple.getPredicate())){
                            topDownDeeper(tagTriple.getObjectID(),tagTriple);
                        }
                    }
                    this.tagTriples.clear();
                    }
                this.tagIDs.clear();
            }
        }
        this.allSortable.clear();
        this.sortablePreds.clear();
        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("            No answers were found using the Sorting Predicates Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
    }

// Top Down Search
    public void topDown(){
        Set<String> tagIDValues = new HashSet<>(); //not only keys
        int count = 0;

    // PopUp Question First layer
            for (String tag : this.tags.keySet()) {

                this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);

                // why only the key set???!
                /*this.IDsList.clear();
                tagIDValues = this.db.nameAlias2IDs(this.tags.get(tag), this.IDsList, tagIDValues);
                this.tagIDs.addAll(tagIDValues);
                tagIDValues.clear();*/

                // System.out.println("This many tag ID "+tagIDs.size());
                for (String tagID : this.tagIDs) {
                    this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
                    if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
                        continue;

                    /*if (tagTriples.size() > this.sizeLimit){
                        tagTriples.clear();
                        continue;
                    }*/

                    // Search some other predicates other than Name ot Alias or the ones that actually connected to an object
                    // this.otherTagTriples = this.db.ID2TriplesFull(tagID, this.otherTagTriples);
                    // if (this.otherTagTriples == null) // can this happen? an ID has no triple associated with it!
                    //     continue;
                    // for (NTriple t: this.otherTagTriples) {
                    //     if (t.getObjectID().toLowerCase().contains("/"+answer+"/"))
                    //         continue;
                    //     if (t.getObjectID().toLowerCase().contains(answer)){
                    //         System.out.println("    "+t.getPredicate());
                    //         System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    //         System.out.println(t.getObjectID());
                    //         System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    //         this.foundText = true;
                    //     }
                    // }
                    // this.otherTagTriples.clear(); 

                    for (NTriple tagTriple : this.tagTriples) {
                        check = false;
                        check = isConnectedToAnswer(tag, tagTriple);
                        if (!check) 
                            check = isConnectedToAnswerMediator(tag, tagTriple);
                    }
                    this.tagTriples.clear();
                }
                this.tagIDs.clear();
            }

    // PopUp Question Second layer
        if(!this.matched){
            for (String tag : this.tags.keySet()) {
                this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
                this.IDsList.clear();
                tagIDValues = this.db.nameAlias2IDs(this.tags.get(tag), this.IDsList, tagIDValues);
                this.tagIDs.addAll(tagIDValues); // Add also the values of tags!

                // System.out.println( "%%%%%%%%%%%%%%%%%%%" +this.tagIDs.size() + " this many tagIDs");
                for (String tagID : this.tagIDs) {
                    this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
                    if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
                        continue;
                    // System.out.println(this.tagTriples.size());
                    for (NTriple tagTriple : this.tagTriples) {
                        String tempName = this.db.ID2name(tagTriple.getObjectID());
                        // storeGoodTriples(this.secondTagNames,tempName, tagTriple);
                        // System.out.println(tag + " " +tagTriple);
                        topDownDeeper(tagTriple.getObjectID(),tagTriple);
                    }
                    this.tagTriples.clear();
                    }
                this.tagIDs.clear();
            }

        // printGoodTriples(this.secondTagNames);
        // this.secondTagNames.clear();
        // this.goodSecondTriples.clear();
        }
        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("            No answers were found using the Brute-Force Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
    }

    public void topDownDeeper(String objID, NTriple justinCase){ //just in case we found the answer here.
        List<NTriple> tagtagTriples = new ArrayList<>();
        List<NTriple> tagMedTriples = new ArrayList<>();
        Map<String, NTriple> mediatorTagTriples = new HashMap<>();

        if (objID == null)
            return; 
        tagtagTriples = this.db.ID2Triples(objID, tagtagTriples);
        if (tagtagTriples == null) // can this happen? an ID has no triple associated with it!
            return;

        // Search some other predicates other than Name ot Alias or the ones that actually connected to an object
        /*this.otherTagTriples = this.db.ID2TriplesFull(objID, this.otherTagTriples);
        if (this.otherTagTriples == null) // can this happen? an ID has no triple associated with it!
            return;
        for (NTriple t: this.otherTagTriples) {
            if (t.getObjectID().toLowerCase().contains("/"+answer+"/"))
                continue;
            if (t.getObjectID().toLowerCase().contains(answer)){
                System.out.println("    "+t.getPredicate() + " ID "+ objID);
                System.out.println("~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2");
                System.out.println(t.getObjectID());
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                this.foundText = true;
            }
        }*/        
        if (! this.objectIDnames.containsKey(objID))
            this.objectIDnames.put(objID,this.db.ID2name(objID));
        // this.allSortable.put((this.objectIDnames.get(objID) + ": " + objID),sortedPredicates(this.objectIDnames.get(objID), objID));
        for (NTriple tagtagTriple : tagtagTriples) {
            check = false;
            check = isConnectedToAnswer("TAGID:"+objID, tagtagTriple);
            if(check){
                if (! this.secondMatches.contains(justinCase)){
                    System.out.printf("     The root triple is : %s \n",justinCase.toString());
                }
                this.secondMatches.add(justinCase);
                // storeGoodTriples (this.goodSecondTriples ,this.objectIDnames.get(objID), tagtagTriple);
            }
            if (!check){
                check = isConnectedToAnswerMediator("TAGID:"+objID, tagtagTriple);
                // System.out.println(check);
                if(check){
                    if (! this.secondMatches.contains(justinCase)){
                        System.out.printf("     The root triple is : %s \n",justinCase.toString());
                    }
                    this.secondMatches.add(justinCase);
                    // storeGoodTriples (this.goodSecondTriples, this.objectIDnames.get(objID), tagtagTriple);
                }
            }
        }
        this.otherTagTriples.clear();
        tagtagTriples.clear();
        tagMedTriples.clear();
        mediatorTagTriples.clear();
    }

    // if the object of the tagTriple has an ID matching an answer ID
    private boolean isConnectedToAnswer(String tag, NTriple tagTriple){
        // System.out.println(tag + " " +tagTriple);
        boolean result = false;
        String tripleSub = tagTriple.getSubjectID();
        String tripleObj = tagTriple.getObjectID();
        if (this.answerIDs.contains(tagTriple.getObjectID())) {
            if(!tag.startsWith("TAGID:")) 
                tagTriple.setSubject(tag);
            else{
                if (! this.objectIDnames.containsKey(tripleSub))
                    this.objectIDnames.put(tripleSub,this.db.ID2name(tripleSub));
                tagTriple.setSubject(this.objectIDnames.get(tripleSub));
            }
            tagTriple.setObject(this.answer);
            this.match.add(tagTriple.getSubject());
            this.match.add(tagTriple.getPredicate());
            this.match.add(null);
            this.match.add(tagTriple.getObject());
            if (!this.matches.contains(this.match)) {
                this.matched = true;
                this.matches.add(this.match);

                //not correct for second layer
                // System.out.printf("MATCHED1: %s | %s\n", this.tags.get(tag), tagTriple.toString());
                System.out.printf("The match is : %s \n",this.match);
                System.out.printf("Match is from triple : %s \n",tagTriple);
                // System.out.printf("The AIDs are : %s \n",String.join(",", answerIDs));
                // System.out.printf("The ID i s : %s from %d answers\n",tagTriple.getObjectID(),this.answerIDs.size());
            }
            result = true;
        }
        // else{
        //     if (!this.objectIDnames.containsKey(tripleObj))
        //         this.objectIDnames.put(tripleObj,this.db.ID2name(tripleObj));
        //     String ans = this.objectIDnames.get(tripleObj).toLowerCase();
        //     if(ans.contains(this.answer)){
        //         System.out.println("The answer is contained in "+ ans + " from the tag " + tagTriple);
        //         this.contained = true;
        //     }
        // }
        this.match.clear();
        return result;
    }

    //if the object of the tagTriple has an ID matching a mediator
    private boolean isConnectedToAnswerMediator(String tag, NTriple tagTriple){
        // System.out.println(tag + " " +tagTriple);
        boolean result = false;
        String tripleSub = tagTriple.getSubjectID();
        String tripleObj = tagTriple.getObjectID();
        if (this.mediatorTriples.containsKey(tagTriple.getObjectID())) { 
            if(!tag.startsWith("TAGID:")) 
                tagTriple.setSubject(tag);
            else{
            if (! this.objectIDnames.containsKey(tripleSub))
                this.objectIDnames.put(tripleSub,this.db.ID2name(tripleSub));
            tagTriple.setSubject(objectIDnames.get(tripleSub));
                // tagTriple.setSubject("NONAME " + temp);
            }
            this.mediatorTriple = this.mediatorTriples.get(tagTriple.getObjectID()); //no object name for mediatorTriple because mediator
            this.mediatorTriple.setSubject(this.answer);
            this.match.add(tagTriple.getSubject());
            this.match.add(tagTriple.getPredicate());
            this.match.add(mediatorTriple.getPredicate());
            this.match.add(mediatorTriple.getSubject()); // this should be the object (aka the answer), because we stored answerTriple here the subject is the asswer (aka desired object)
            if (!this.matches.contains(this.match)) {
                this.matched = true;
                this.matches.add(this.match);

                //not correct for second layer
                // System.out.printf("MATCHED2: %s | %s | %s\n", this.tags.get(tag), tagTriple.toString(), this.mediatorTriple.toReverseString());
                System.out.printf("The match is : %s \n",this.match);
                System.out.printf("Match is from triple : %s \n",tagTriple);
                // System.out.printf("The MIDs are : %s \n",String.join(",", mediatorTriples.keySet()));
                // System.out.printf("The ID is : %s from %d mediators\n",tagTriple.getObjectID(), this.mediatorTriples.size());
            }
            result = true;
        }
        // else{
        //     String tagObj= tagTriple.getObjectID();
        //     if (! this.objectIDnames.containsKey(tagObj))
        //         this.objectIDnames.put(tagObj,this.db.ID2name(tagObj));
        //     String ans = this.objectIDnames.get(tagObj).toLowerCase();
        //     if(ans.contains(this.answer)){
        //         System.out.println("The answer is contained in "+ ans + " from the tag " + tagTriple);
        //         this.contained = true;
        //     }
        // }
        this.match.clear();
        return result;
    }


// Other methods

    private void storeGoodTriples (Map<String, List<NTriple>> triples,String name, NTriple triple){
            if(triples.get(name) == null){
            List<NTriple> temp = new ArrayList<>();
            temp.add(triple);
            triples.put(name,temp);
            }else{
                triples.get(name).add(triple);
            }
    }

    private void printGoodTriples (Map<String, List<NTriple>> triples){
        if (triples == null)
            return;
        if (triples.size() == 0)
            return;
        for (String entry: triples.keySet()){
            System.out.println("    "+entry);
            for (NTriple t: triples.get(entry)){
                System.out.println("        "+t.getPredicate());
            }
        }
    }

    private static Map<String, NTriple> deepCopyMap(Map<String, NTriple> original){
        Map<String, NTriple> copy = new HashMap<String, NTriple>();
        for (Map.Entry<String, NTriple> entry : original.entrySet()){
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private static Set<String> deepCopySet(Set<String> original){
        Set<String> copy = new HashSet<String>();
        for (String entry : original){
            copy.add(entry);
        }
        return copy;
    }

    private static Set<List<String>> deepCopyList(Set<List<String>> original){
        List<String> copy = new ArrayList<String>();
        Set<List<String>> copy2 = new HashSet<List<String>>();

        for (List<String> entry2 : original){
            for (String entry : entry2){
                copy.add(entry);
            }
            copy2.add(copy);
        }
        return copy2;
    }

    public void cleanUp(){
        this.IDsList.clear();
        this.mediatorTriples.clear();
        this.answerIDs.clear();
        this.tags.clear();
        this.objectIDnames.clear();
        this.matches.clear();
        this.secondMatches.clear();
        this.sortablePreds.clear();
        this.commonTagTriples.clear();
        System.gc();
    }

    public Map<String,Map<String,List<NTriple>>> sortedPredicates(String tag, String tagID){
        Map<String,Map<String,List<NTriple>>> temp = new HashMap<>();
        Map<String,Map<String,List<NTriple>>> temp15 = new HashMap<>();
        Map<String,List<NTriple>> temp2 = new HashMap<>();

        this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
        if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
            return null;
        this.preds = new Predicates(tagID, this.db);
        temp = new HashMap<>();
        temp15 = new HashMap<>();
        this.preds.comparablePredicate(temp);
        this.preds.printPredicate();
        for (String s: temp.keySet()){
			if(this.sortablePreds == null){
				System.out.println("No reference to sortablePreds for " + tag + " with ID " + tagID);
			}
            this.sortablePreds.add(s);
            temp2 = new HashMap<>();
            temp2 = temp.get(s);
            temp15.put(s,temp2);
        }
		if(this.sortablePreds.size() == 0){
			System.out.println("No sortable predicates for " + tag + " with ID " + tagID);
		}else if(this.sortablePreds.size() > 0){
            System.out.println(this.sortablePreds.size() + " sortable predicates, listed below:");
            System.out.println(this.sortablePreds);
        }

        this.preds.cleanUp();
        return temp15;
    }

    public boolean isMatched(){
        return this.matched;
    }

    public boolean isAnswer(String objectID){
        return this.answerIDs.contains(objectID);
    }

    public boolean isMedAnswer(String objectID){
        return this.mediatorTriples.containsKey(objectID);
    }

    public boolean isInFB(){
        return this.exists;
    }

    public boolean isAnswerInText(){
    return this.foundText;
    }

    public boolean isAnswerContained(){
    return this.contained;
    }

    public Map<String, NTriple> getMediatorsMap(){
        Map<String, NTriple> copy = deepCopyMap(this.mediatorTriples);
        return copy;
    }

    public Set<String> getAnswerIDsSet(){
        Set<String> copy = deepCopySet(this.answerIDs);
        return copy;
    }

    public Set<List<String>> getMatches(){
        Set<List<String>> copy = deepCopyList(this.matches);
        return copy;
    }

    public int getMatchesSize(){
        return this.matches.size();
    }

    public int getAnswerIDsSize(){
        return this.answerIDs.size();
    }

    public String getQuestionPackage(String question){
        String result = new String(question + " | " + this.answer);
        for (String tag : tags.keySet()) {
            result = result + (" | " + tag);
            result = result + (" | " + tags.get(tag));
        }
        return result;
    }
}
