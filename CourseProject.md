# NetGameSim to MPI Distributed Algorithms Project

This project uses synthetic network graphs as the input for a distributed MPI application that runs two classic distributed algorithms on a graph that is partitioned across multiple MPI ranks. You will use NetGameSim to generate random graphs with controlled properties, assign edge weights, partition the graph across ranks, then run leader election and shortest path computations using message passing between ranks.

Upstream resources used by this project are shown below.

```
NetGameSim repository
https://github.com/0x1DOCD00D/NetGameSim

NetGameSim code walkthru video by Prof. Grechanik
https://www.youtube.com/watch?v=6fdazJBkdjA&t=2658s
```

## What you will build

The end to end workflow is shown below.

```
NetGameSim graph generation
        |
        v
Graph enrichment
  - positive edge weights
  - optional node ids and metadata
        |
        v
Graph partitioning across MPI ranks
  - node ownership map
  - ghost nodes for cross edges
        |
        v
MPI program execution
  - distributed leader election
  - distributed Dijkstra shortest paths
        |
        v
Metrics, logs, and experiment report
```

The key design shift in this project is that a graph node is not forced to be an MPI rank. Instead, each MPI rank owns a subset of graph nodes, which is how real distributed systems often work when the number of logical entities is larger than the number of processes.

## Graph generation and enrichment

You will use NetGameSim to generate graphs that satisfy constraints such as connectedness, average degree, and size. After generation, you will enrich the graph with data needed by the algorithms.

* Each node must have a unique integer id in the range 0 to N minus 1.
* Each edge must have a positive weight, since Dijkstra assumes nonnegative weights.
* The graph must be connected, otherwise leader election and shortest paths become ambiguous across components.

Weights can be assigned using a simple distribution, for example uniform integers in the range 1 to 20, or a heavier tailed distribution to create more interesting shortest path structure.

## Graph partitioning across MPI ranks

You will partition the nodes across MPI ranks. Each rank stores its owned nodes and their adjacency lists. Edges that cross rank boundaries require explicit inter rank messaging.

A correct implementation includes the following concepts.

* An owner map that returns the rank that owns a given node id.
* A local adjacency structure for owned nodes, plus neighbor ids that may be remote.
* Ghost node entries for remote neighbors so a rank can track tentative distances and leader candidates that originate outside its partition.

You may start with a simple partitioning strategy, such as contiguous ranges of node ids, then optionally implement a better partitioner that reduces edge cuts.

## Distributed leader election

You will implement leader election on a general connected graph. The project expects an algorithm that converges correctly regardless of cycles, as long as the graph is connected and node ids are unique.

A practical baseline is a FloodMax style election.

* Each node starts with its own id as the current leader candidate.
* In each synchronous round, a node sends its current candidate id to all neighbors.
* A node updates its candidate to the maximum id received so far.
* After enough rounds for information to propagate across the diameter, all nodes agree on the same maximum id, which is the leader.

Since the graph is partitioned, each MPI rank performs local updates for its owned nodes, then exchanges boundary messages for candidate ids along cross rank edges. You can terminate using either a known upper bound on rounds, or a convergence check that uses an MPI collective to detect that no node changed its candidate in the last round.

Your implementation should produce the leader id and also confirm agreement by checking that all nodes end with the same leader id.

## Distributed Dijkstra shortest path

You will implement a distributed variant of Dijkstra shortest paths. This project assumes positive edge weights. The algorithm computes shortest distances from a chosen source node to all nodes.

A practical MPI approach is a parallel Dijkstra with global minimum selection.

* Each rank maintains tentative distances for its owned nodes.
* Each rank maintains a local frontier of unsettled nodes, typically represented with a local priority queue.
* On each iteration, every rank proposes its current best local unsettled node, then all ranks participate in a global minimum reduction to choose the single globally best next node to settle.
* The chosen node is broadcast, and the owning rank relaxes its outgoing edges.
* If a relaxation targets a remote owned neighbor, the rank sends a distance update message to the neighbor owner rank.

This algorithm is not fully decentralized, since it uses a collective each iteration, but it is a correct and scalable MPI baseline that clearly demonstrates the challenges of distributed shortest path computation on partitioned graphs.

Your implementation should report distances, number of iterations, message counts, and total runtime.

## Repository layout

A suggested layout is shown below.

```
/
  netgamesim/                 Upstream or forked NetGameSim code
  tools/
    graph_export/             Export graph with weights to a portable format
    partition/                Partition graph and emit ownership map
  mpi_runtime/
    src/                      C/C++ MPI runtime and algorithms
    include/                  Shared headers for graph and messaging
    CMakeLists.txt            Build configuration for mpicc or mpicxx
  configs/                    Example graph and run configurations
  experiments/                Scripts to reproduce runs and collect results
  outputs/                    Generated graphs, partitions, logs, summaries
  REPORT.md                   Experiment writeup
```

## Running an end to end example

A typical workflow is shown below.

```bash
# 1) Generate and enrich a connected weighted graph
./tools/graph_export/run.sh configs/small.conf outputs/graph.json

# 2) Partition the graph across ranks
./tools/partition/run.sh outputs/graph.json --ranks 8 --out outputs/part.json

# 3) Build the MPI runtime
cmake -S mpi_runtime -B build
cmake --build build

# 4) Run leader election
mpirun -n 8 ./build/ngs_mpi --graph outputs/graph.json --part outputs/part.json --algo leader --rounds 200

# 5) Run distributed Dijkstra
mpirun -n 8 ./build/ngs_mpi --graph outputs/graph.json --part outputs/part.json --algo dijkstra --source 0
```
## Submission Dealine

Thursday, April, 16 at 10AM by submitting the URL to your Github repository via the posted Teams assignment. Make sure that you add the TA as the collaborator on your repo using his email as the ID: rvarde2@uic.edu.

## Deliverables

You are expected to produce working code, reproducible runs, and a short report. The required deliverables are listed below.
* A NetGameSim based generator is used to output connected weighted graphs.
* A partitioning tool that assigns each node to exactly 1 MPI rank and emits an ownership map.
* An MPI runtime in C or C++ that loads the graph and partition, then runs leader election and distributed Dijkstra.
* A metrics summary that includes runtime, total messages sent, bytes sent, and per algorithm iteration counts.
* At least 2 experiments that compare results across different graphs or different partitioning strategies.
* A REPORT.md that describes the design, the algorithm choices, and the experiment results. 

## Grading rubric

The maximum grade for this project is 20%. Your grade starts at 20% and deductions are subtracted from it. If a requirement says 2% is lost, your grade becomes 18%. The minimum grade cannot be less than 0%.

### Automatic zero conditions

If any item below is true, the grade for this project is 0%.

* The core functionality does not work, or it is not implemented as specified in your documentation.
* Your submission is mostly copied basic graph manipulation examples from other repos and does not implement the required project pipeline and algorithms.

Core functionality for this project means all items below work on at least two nontrivial generated graphs.
* Ensure that all operations performed on generated graphs are reproducible by enforcing deterministic execution through a fixed seed. Any algorithm that involves randomness during graph processing such as sampling, traversal tie-breaking, layout adjustments, stochastic optimization, or probabilistic pruning must be controlled by a seedable random number generator. The seed must be initialized at the beginning of execution and consistently used across all components that introduce non-determinism.
* Additionally, eliminate implicit sources of variability by enforcing stable ordering where applicable, for example when iterating over nodes, edges, or adjacency structures. The program should expose the seed as a configurable parameter if you use some randomization inside your program and record it alongside outputs so that the exact sequence of operations and resulting graph transformations can be reproduced precisely on the same input graph.
* The graph is partitioned across multiple MPI ranks.
* Leader election runs to completion and all nodes agree on the same leader.
* Distributed Dijkstra runs to completion and produces correct shortest path distances from a chosen source.

### Deductions

Deductions are applied after passing the zero conditions.

Language and tooling

* Using a different language stack for the core deliverables than the required one in your README, for example replacing the MPI C or C++ runtime with Python, is a 10% penalty.
* Building requires manual IDE clicks with no command-line build and run path, up to 5% lost.

Testing

* Fewer than 5 unit or integration tests that actually execute code paths in your partitioning and algorithms, up to 10% lost.
* Tests exist but are not runnable from a documented command, up to 5% lost.
* No correctness checks in tests, for example leader agreement or distance validation on known graphs, up to 5% lost.

Documentation and usability

* No clear instructions in README.md on how to install dependencies, build, and run, up to 10% lost.
* Documentation exists but is insufficient to understand your design choices, data formats, partition model, message formats, and how to reproduce experiments, up to 15% lost.
* Missing explanation of the assumptions required for correctness, for example positive weights for Dijkstra, up to 5% lost.
* Missing a small end-to-end example that a grader can run quickly, up to 5% lost.

Robustness

* The program crashes, hangs, or deadlocks before completing leader election or Dijkstra, up to 15% lost.
* The program completes sometimes but fails intermittently due to nondeterminism that you did not control or explain, up to 10% lost.
* Missing input validation and error handling for file loading, rank counts, or malformed graphs, up to 5% lost.

Logging, metrics, and reproducibility

* Logging is not used in the runtime and tools, up to 5% lost.
* No metrics summary is produced for at least message counts and runtime per algorithm, up to 5% lost.
* Hardcoding input values in source code instead of using configuration or CLI arguments, up to 5% lost.
* Missing seed capture and replay support for graph generation and algorithm runs, up to 5% lost.

Code quality deductions
These are small but cumulative. They are meant to punish sloppy coding, not legitimate engineering tradeoffs.

* For each mutable state item that is not justified in comments, for example global mutable structures or shared mutable collections, 0.3% lost each.
* For each low-level loop that is clearly avoidable and harms readability, for example manual index loops over a collection when a safer alternative exists in the chosen language, 0.5% lost each.
* For each copy-pasted block that should have been a function, 0.5% lost each.

## Additional deductions based on IntelliJ/CLion inspections and static analysis

These items are applied based on what IntelliJ/CLion inspections and the build output report. They are easy to avoid, so they are easy to grade.

Warnings and errors

* Any compilation errors remaining in the default build configuration is 10% lost.
* Each unresolved warning of severity warning or higher in project code, 0.2% lost each, capped at 5%.
* Each unused variable, unused import, dead code block, or ignored return value flagged by inspections, 0.2% lost each, capped at 3%.

MPI correctness and resource hygiene

* Any mismatched collective usage that can cause a hang, for example not all ranks call the same collective in the same order, is up to 10% lost.
* Not freeing dynamically created MPI communicators or other MPI resources when applicable, up to 3% lost.
* Not checking for MPI error codes in critical calls where failure would corrupt results, up to 3% lost.

Complexity and maintainability

* Each function that exceeds a reasonable complexity threshold based on inspections, for example a single function that implements parsing, partitioning, messaging, and algorithm logic all at once, 0.5% lost each, capped at 5%.
* Each TODO or FIX ME left in core algorithm paths without an explanation in the report, 0.5% lost each, capped at 3%.

Correctness hazards

* Each inspection flagged issue that can plausibly change results, for example integer overflow on distances, uninitialized reads, or out-of-bounds access, 1% lost each, capped at 10%.

### Notes on grading

* If multiple deductions overlap, the larger one is applied when they describe the same failure mode.
* If deductions exceed 20%, the final grade is still 0%.

### Suggested experiments and extensions

This framework is intentionally reusable. After the required algorithms work, you can implement deeper experiments.

* Compare at least two partitioners, then measure how edge cuts affect message counts and runtime.
* Measure scaling by fixing the graph and increasing ranks, then explain when collectives dominate Dijkstra runtime.
* Add a second leader election algorithm, for example a spanning tree based echo election, then compare rounds and messages.
* Add optional asynchronous relaxation for shortest paths, then compare correctness checks and convergence behavior.

## Attribution and licensing

This project builds on NetGameSim by Prof. Grechanik and follows the upstream license and attribution requirements. The upstream repository and walkthrough video are listed near the top of this README.
