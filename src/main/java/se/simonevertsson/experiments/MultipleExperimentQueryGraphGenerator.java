package se.simonevertsson.experiments;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import se.simonevertsson.query.QueryGraph;
import se.simonevertsson.query.QueryNode;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by simon on 2015-07-13.
 */
public class MultipleExperimentQueryGraphGenerator {

    private final ArrayList<Node> allNodes;
    private final int minRelationships;
    private final int maxNodeCount;
    private int currentNodeCount;
    private final int nodeCountDecrease;
    private int currentNodeAlias;
    private HashMap<Long, QueryNode> visitedNodes;
    private HashMap<Long, Relationship>  visitedRelationships;
    private Queue<Relationship> relationshipQueue;
    private int currentMinRelationships;
    private QueryGraph oldQueryGraph;
    private int preferredIterationCount;

    public MultipleExperimentQueryGraphGenerator(ArrayList<Node> allNodes, int maxNodeCount, int nodeCountDecrease, int minRelationships) {
        this.allNodes = allNodes;
        this.maxNodeCount = maxNodeCount;
        this.nodeCountDecrease = nodeCountDecrease;
        this.minRelationships = minRelationships;
    }

    public ArrayList<QueryGraph> generate(int preferredIterationCount) {
//        this.oldQueryGraph = oldQueryGraph;
        this.preferredIterationCount = preferredIterationCount;
        Random random = new Random();
        int startIndex = random.nextInt(this.allNodes.size());
        System.out.println("Generating query graph from start index " + startIndex);

        return generateFromStartIndex(startIndex);
    }

    public ArrayList<QueryGraph> generateFromStartIndex(int startIndex) {
        return generateQueryGraphFromGivenStartIndex(startIndex);
    }

    private ArrayList<QueryGraph> generateQueryGraphFromGivenStartIndex(int startIndex) {
        int rootNodeAttempts = 0;
        ArrayList<QueryGraph> queryGraphs = new ArrayList<>();

        int rootNodeIndex = 0;

        while (rootNodeAttempts < this.allNodes.size() &&
                queryGraphs.size() < (this.preferredIterationCount*(this.maxNodeCount-1))) {
            initializeGenerationAttempt(this.maxNodeCount, this.minRelationships);
            QueryGraph queryGraph = new QueryGraph();

            rootNodeIndex = (startIndex + rootNodeAttempts) % this.allNodes.size();
            Node rootNode = this.allNodes.get(rootNodeIndex);

            addNodeToQueryGraph(queryGraph, rootNode);

            if (generateQueryGraph(queryGraph, rootNode) != null &&  queryGraph.relationships.size() >= this.currentMinRelationships ) {
//                if(this.oldQueryGraph == null || !oldQueryGraph.toCypherQueryString().equals(queryGraph.toCypherQueryString()) ) {
                if(!queryGraphs.contains(queryGraph)) {
                    System.out.println("Generated query graph with " + this.currentNodeCount + " nodes and minimum " + this.currentMinRelationships + " relationships.");
                    //                queryGraphs.add(queryGraph);
                    //                break;
                    queryGraphs.add(queryGraph);

                    System.out.println("Root node found at index: " + rootNodeIndex);

                    int nodeCount = this.currentNodeCount - this.nodeCountDecrease;
                    int minRelationships = this.currentMinRelationships - 1;

                    while (nodeCount >= 2) {


                        initializeGenerationAttempt(nodeCount, minRelationships);
                        queryGraph = new QueryGraph();

                        rootNode = this.allNodes.get(rootNodeIndex);

                        addNodeToQueryGraph(queryGraph, rootNode);

                        if (generateQueryGraph(queryGraph, rootNode) != null &&  queryGraph.relationships.size() >= this.currentMinRelationships) {
                            System.out.println("Generated query graph with " + nodeCount + " nodes and minimum " + minRelationships + " relationships.");
                            queryGraphs.add(queryGraph);
                            nodeCount = this.currentNodeCount - this.nodeCountDecrease;
                            minRelationships = this.currentMinRelationships - 1;
                        } else {
                            minRelationships = this.currentMinRelationships - 1;
                        }
                    }
                }
            }



            rootNodeAttempts++;
        }

        return queryGraphs;
    }

    private QueryGraph generateQueryGraph(QueryGraph queryGraph, Node visitedNode) {
        for(Relationship relationship : visitedNode.getRelationships()) {
            if (this.visitedNodes.size() == this.currentNodeCount && this.visitedRelationships.size() >= this.minRelationships) {
                /* The query graph has been generated */
                return queryGraph;
            }

            if(this.visitedRelationships.get(relationship.getId()) == null) {
                if(this.visitedNodes.size() == this.currentNodeCount) {
                    /* Add relationships until stopping condition is fulfilled */
                    addRelationshipWithVisitedNodesToQueryGraph(queryGraph, relationship);
                } else if(this.visitedRelationships.size() < this.minRelationships) {
                    /* Add nodes and relationships until stopping condition is fulfilled */
                    addRelationshipAndUnvisitedNodeToQueryGraph(queryGraph, relationship);
                    Node unvisitedNode = null;
                    if (relationship.getStartNode().getId() == visitedNode.getId()) {
                        unvisitedNode = relationship.getEndNode();
                    } else {
                        unvisitedNode = relationship.getStartNode();
                    }
                    generateQueryGraph(queryGraph, unvisitedNode);
                }

            }

        }

        if (this.visitedNodes.size() == this.currentNodeCount) {
            /* The query graph has been generated */
            return queryGraph;
        } else {
            if(this.visitedRelationships.size() >= this.minRelationships) {
                return queryGraph;
            } else {
                return null;
            }
        }
    }



//    private List<Relationship> createRelationshipList(Node node) {
//        List<Relationship> relationshipList = new ArrayList<>();
//        if(node != null) {
//            int addedRelationships = 0;
//            for (Relationship relationship : node.getRelationships(Direction.BOTH)) {
//                if(visitedRelationships.get(relationship.getId()) == null) {
//                    relationshipList.add(relationship);
//                    addedRelationships++;
//                }
//            }
//        }
//        return relationshipList;
//    }

    private void addRelationshipAndUnvisitedNodeToQueryGraph(QueryGraph queryGraph, Relationship currentRelationship) {
        Node startNode = currentRelationship.getStartNode();
        Node endNode = currentRelationship.getEndNode();
        QueryNode startQueryNode;
        QueryNode endQueryNode;
        if (visitedNodes.get(startNode.getId()) == null) {
            startQueryNode = addNodeToQueryGraph(queryGraph, startNode);
            endQueryNode = visitedNodes.get(endNode.getId());

        } else if (visitedNodes.get(endNode.getId()) == null) {
            startQueryNode = visitedNodes.get(startNode.getId());
            endQueryNode = addNodeToQueryGraph(queryGraph, endNode);
        } else {
            startQueryNode = visitedNodes.get(startNode.getId());
            endQueryNode = visitedNodes.get(endNode.getId());
        }


        Relationship queryRelationship = startQueryNode.createRelationshipTo(endQueryNode, currentRelationship.getId(), currentRelationship.getType());

        queryGraph.relationships.add(queryRelationship);
        visitedRelationships.put(currentRelationship.getId(), queryRelationship);
    }

    private void addRelationshipWithVisitedNodesToQueryGraph(QueryGraph queryGraph, Relationship relationship) {
        QueryNode startQueryNode = visitedNodes.get(relationship.getStartNode().getId());
        QueryNode endQueryNode = visitedNodes.get(relationship.getEndNode().getId());
        if(startQueryNode != null && endQueryNode != null ) {
            Relationship queryRelationship = startQueryNode.createRelationshipTo(endQueryNode, relationship.getId(), relationship.getType());
            queryGraph.relationships.add(queryRelationship);
            visitedRelationships.put(relationship.getId(), queryRelationship);
        }
    }

    private void initializeGenerationAttempt(int nodeCount, int minRelationships) {
        this.relationshipQueue = new LinkedList<Relationship>();
        this.visitedRelationships = new HashMap<>();
        this.visitedNodes = new HashMap<>();
        this.currentNodeAlias = 0; //start with alias letter 'A'
        this.currentNodeCount = nodeCount;
        this.currentMinRelationships = minRelationships;
    }

    private QueryNode addNodeToQueryGraph(QueryGraph queryGraph, Node node) {
        QueryNode queryNode = new QueryNode(node);
        queryGraph.nodes.add(queryNode);
        queryGraph.aliasDictionary.insertAlias(queryNode, ExperimentUtils.createAlias(this.currentNodeAlias));
        this.currentNodeAlias++;
        visitedNodes.put(queryNode.getId(), queryNode);
        return queryNode;
    }

    private String createAlias() {
        StringBuilder builder = new StringBuilder();

        int aliasLength = (this.currentNodeAlias / 26) + 1;
        int rest = this.currentNodeAlias;
        for(int i = 0; i < aliasLength; i++) {
            int aliasCharacter = rest % 26;
            builder.append((char)(65 + aliasCharacter));
            rest /= 26;
        }

        return builder.reverse().toString();
    }

//    private void addNodeRelationshipsToQueue(Node node) {
//        if(node != null) {
//            int addedRelationships = 0;
//            for (Relationship relationship : node.getRelationships(Direction.BOTH)) {
//                if(visitedRelationships.get(relationship.getId()) == null) {
//                    relationshipQueue.add(relationship);
//                    addedRelationships++;
//                    if (addedRelationships >= maxRelationshipsPerLevel) ;
//                    {
//                        break;
//                    }
//                }
//            }
//        }
//    }

}
