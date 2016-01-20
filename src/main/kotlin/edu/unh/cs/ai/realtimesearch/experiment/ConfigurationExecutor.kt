package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleEnvironment
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime_.LssLrtaStarPlanner

/**
 * @author Bence Cserna (bence@cserna.net)
 */
object ConfigurationExecutor {
    fun executeConfiguration(experimentConfiguration: ExperimentConfiguration): List<ExperimentResult> {
        val domainName: String = experimentConfiguration.getDomainName()

        return when (domainName) {
            "sliding tile puzzle" -> executeSlidingTilePuzzle(experimentConfiguration)
            "vacuum world" -> executeVacuumWorld(experimentConfiguration)
            else -> listOf(ExperimentResult(experimentConfiguration, errorMessage = "Unknown domain type: $domainName"))
        }
    }

    private fun executeVacuumWorld(experimentConfiguration: ExperimentConfiguration): List<ExperimentResult> {
        val rawDomain: String = experimentConfiguration.getRawDomain()
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(rawDomain.byteInputStream())
        val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

        return executeDomain(experimentConfiguration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState, vacuumWorldEnvironment)
    }

    private fun executeSlidingTilePuzzle(experimentConfiguration: ExperimentConfiguration): List<ExperimentResult> {
        val rawDomain: String = experimentConfiguration.getRawDomain()
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(rawDomain.byteInputStream())
        val slidingTilePuzzleEnvironment = SlidingTilePuzzleEnvironment(slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)

        return executeDomain(experimentConfiguration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState, slidingTilePuzzleEnvironment)
    }

    private fun <StateType : edu.unh.cs.ai.realtimesearch.environment.State<StateType>> executeDomain(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): List<ExperimentResult> {
        val algorithmName = experimentConfiguration.getAlgorithmName()

        return when (algorithmName) {
            "A*" -> executeAStar<StateType>(experimentConfiguration, domain, initialState, environment)
            "LSS-LRTA*" -> executeLssLrtaStar<StateType>(experimentConfiguration, domain, initialState, environment)
            "RTA" -> executeRealTimeAStar<StateType>(experimentConfiguration, domain, initialState, environment)

            else -> listOf(ExperimentResult(experimentConfiguration, errorMessage = "Unknown algorithm: $algorithmName"))
        }
    }

    private fun <StateType : edu.unh.cs.ai.realtimesearch.environment.State<StateType>> executeAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): List<ExperimentResult> {
        val aStarPlanner = AStarPlanner(domain)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment<StateType>(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : edu.unh.cs.ai.realtimesearch.environment.State<StateType>> executeLssLrtaStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): List<ExperimentResult> {
        val lssLrtaPlanner = LssLrtaStarPlanner(domain)
        val rtsAgent = RTSAgent(lssLrtaPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration), experimentConfiguration.getNumberOfRuns())

        return rtsExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: ExperimentConfiguration): TerminationChecker {
        val terminationCheckerType = experimentConfiguration.getTerminationCheckerType()
        val terminationCheckerParameter = experimentConfiguration.getTerminationCheckerParameter()

        return when (terminationCheckerType) {
            "time" -> TimeTerminationChecker(terminationCheckerParameter.toDouble())
            "calls" -> CallsTerminationChecker(terminationCheckerParameter)

            else -> throw RuntimeException("Invalid termination checker: $terminationCheckerType")
        }
    }

    private fun <StateType : edu.unh.cs.ai.realtimesearch.environment.State<StateType>> executeRealTimeAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): List<ExperimentResult> {
        throw UnsupportedOperationException("not implemented")
    }


}