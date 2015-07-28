package se.simonevertsson.gpu;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLMem;
import org.bridj.Pointer;

import javax.management.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class CandidateRelationshipJoiner {

    private final QueryContext queryContext;
    private final QueryKernels queryKernels;
    private final SolutionCombinationCounter solutionCombinationCounter;
    private final SolutionCombinationGenerator solutionCombinationGenerator;
    private final SolutionValidator solutionValidator;
    private final SolutionPruner solutionPruner;
    private final SolutionInitializer solutionInitializer;

    public CandidateRelationshipJoiner(QueryContext queryContext, QueryKernels queryKernels, BufferContainer bufferContainer) {
        this.queryContext = queryContext;
        this.queryKernels = queryKernels;
        this.solutionInitializer = new SolutionInitializer(queryContext, queryKernels);
        this.solutionValidator = new SolutionValidator(queryKernels, queryContext);
        this.solutionPruner = new SolutionPruner(queryKernels, queryContext);
        this.solutionCombinationCounter = new SolutionCombinationCounter(queryKernels, queryContext);
        this.solutionCombinationGenerator = new SolutionCombinationGenerator(queryKernels,queryContext);
    }

    public PossibleSolutions joinCandidateRelationships(HashMap<Integer, CandidateRelationships> candidateRelationshipsHashMap) throws IOException {
        ArrayList<Integer> visitedQueryRelationships = new ArrayList<Integer>();
        ArrayList<Integer> visitedQueryNodes = new ArrayList<Integer>();

        PossibleSolutions possibleSolutions = solutionInitializer.initializePossibleSolutions(candidateRelationshipsHashMap, visitedQueryRelationships, visitedQueryNodes);


        while( visitedQueryRelationships.size() < candidateRelationshipsHashMap.size()) {
            for (int relationshipId : candidateRelationshipsHashMap.keySet()) {
                if (!visitedQueryRelationships.contains(relationshipId)) {
                    CandidateRelationships candidateRelationships = candidateRelationshipsHashMap.get(relationshipId);
                    int startNodeId = candidateRelationships.getQueryStartNodeId();
                    int endNodeId = candidateRelationships.getQueryEndNodeId();
                    boolean startNodeVisisted = visitedQueryNodes.contains(startNodeId);
                    boolean endNodeVisisted = visitedQueryNodes.contains(endNodeId);

                    int possibleSolutionCount = (int) possibleSolutions.getSolutionElements().getElementCount() / this.queryContext.queryNodeCount;

                    if (startNodeVisisted && endNodeVisisted) {
                        /* Prune existing possible solutions */
                        CLBuffer<Boolean> validationIndicators = solutionValidator.validateSolutions(possibleSolutions, candidateRelationships);

                        Pointer<Boolean> validationIndicatorsPointer = validationIndicators.read(this.queryKernels.queue);

                        int[] outputIndexArray = generateOutputIndexArray(validationIndicatorsPointer, possibleSolutionCount);

                        if (outputIndexArray[outputIndexArray.length - 1] == 0) {
                            return null;
                        }

                        PossibleSolutions prunedPossibleSolutions = solutionPruner.prunePossibleSolutions(
                                possibleSolutions,
                                validationIndicators,
                                outputIndexArray);

                        possibleSolutions = prunedPossibleSolutions;
                        visitedQueryRelationships.add(relationshipId);

                    } else if (startNodeVisisted || endNodeVisisted) {
                        /* Combine candidate edges with existing possible solutions */
                        Pointer<Integer> combinationCountsPointer = this.solutionCombinationCounter.countSolutionCombinations(
                                possibleSolutions,
                                candidateRelationships,
                                startNodeVisisted);

                        int[] combinationIndices = QueryUtils.generatePrefixScanArray(combinationCountsPointer, possibleSolutionCount);

                        PossibleSolutions newPossibleSolutions = solutionCombinationGenerator.generateSolutionCombinations(
                                possibleSolutions,
                                candidateRelationships,
                                startNodeVisisted,
                                combinationIndices);

                        possibleSolutions = newPossibleSolutions;

                        visitedQueryRelationships.add(relationshipId);
                        if (startNodeVisisted) {
                            visitedQueryNodes.add(endNodeId);
                        } else {
                            visitedQueryNodes.add(startNodeId);
                        }
                    }
                }
            }
        }

        return possibleSolutions;

    }

    public int[] generateOutputIndexArray(Pointer<Boolean> validationIndicatorsPointer, int possibleSolutionCount) {
        int[] outputIndexArray = new int[possibleSolutionCount + 1];
        int validSolutionCount = 0;

        if (validationIndicatorsPointer.get(0)) {
            validSolutionCount++;
        }

        for (int i = 1; i < possibleSolutionCount; i++) {
            int nextElement = validationIndicatorsPointer.get(i - 1) ? 1 : 0;
            outputIndexArray[i] = outputIndexArray[i - 1] + nextElement;
            if (validationIndicatorsPointer.get(i)) {
                validSolutionCount++;
            }
        }

        outputIndexArray[outputIndexArray.length - 1] = validSolutionCount;
        return outputIndexArray;
    }

}