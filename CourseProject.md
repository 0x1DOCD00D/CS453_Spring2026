# NetGameSim to MPI Graph Workload Generator

This project turns synthetic network graphs into runnable MPI workloads. It uses NetGameSim to generate random graphs with controlled properties, extends those graphs with message semantics, then converts the result into a parallel MPI program in C or C++. The generated program is compiled and executed so each MPI rank behaves like a process hosted on a graph node, sending and receiving messages over graph edges while simulating computation.

Upstream resources used by this project are shown below.

[NetGameSim repository](https://github.com/0x1DOCD00D/NetGameSim) and NetGameSim overview and code walkthru [video](https://www.youtube.com/watch?v=6fdazJBkdjA&t=2658s)

## What you will build

The end-to-end pipeline is shown below.

```
NetGameSim graph generation
        |
        v
Graph enrichment
  - edge message-type labels
  - node message-production PDFs
        |
        v
Graph to MPI conversion
  - rank <-> node mapping
  - edge <-> channel mapping
        |
        v
Generated C/C++ MPI program
        |
        v
Compile and run
  - collect metrics and logs
  - compare across graphs and workloads
```

The core idea is to treat a graph as a distributed system blueprint.

* Each node becomes one MPI rank.
* Each edge becomes a communication channel between two ranks.
* Each channel is annotated with which message types are allowed to traverse it.
* Each node has a probability distribution function over the message types it produces.

## Graph generation requirements

NetGameSim already supports large-scale random graph generation and related tooling. In this project, students treat NetGameSim as the graph engine and add a repeatable way to request graphs that match constraints such as connectivity, average degree, diameter bounds, and clustering behavior.

The minimum expectation is that a run is reproducible from a single configuration file and a seed, and the generated artifacts contain enough metadata to regenerate the same graph later.

## Workload model

### Message types and edge labels

A message type is an application-level label, not an MPI datatype. Examples include `heartbeat`, `control`, `request`, `response`, and `data`. Each edge is labeled with a subset of allowed message types so that the topology constrains which messages can be sent where.

This project adds an edge-labeling pass to NetGameSim so the output graph explicitly includes, per edge, the allowed message types and any optional channel parameters you choose to model, such as simulated delay or drop probability.

### Node PDFs for message production

Each node is assigned a PDF that describes what it produces. At minimum, this is a categorical distribution over message types, with probabilities that sum to 1.0. More advanced models can incorporate a rate model, for example a Poisson arrival process for messages, plus a conditional destination choice model.

The goal is to generate heterogeneous workloads, where different nodes behave differently, while keeping the workload specification explicit and machine-readable.

## MPI mapping

This project treats MPI as the execution backend and uses the graph as the communication topology.

A correct implementation supports two channel backends.

One backend creates an MPI communicator per edge. This makes the graph-to-MPI mapping very literal, but it can stress communicator creation and memory when the graph is dense.

The other backend keeps a single communicator and uses tags to represent edges. This scales better and provides a useful comparison point for performance experiments.

Regardless of backend, the runtime should avoid deadlock by using non-blocking sends and a receive loop driven by probing or posted receives. A simple and robust pattern is to send with `MPI_Isend`, poll with `MPI_Iprobe`, and drain with `MPI_Recv` until no more messages are available.

## Repository layout

A suggested repository layout is shown below.

```
/
  netgamesim/                 Upstream or forked NetGameSim code
  tools/
    graph_export/             Export enriched graphs to a portable format
    graph2mpi/                Generate MPI code or headers from the graph
  mpi_runtime/
    src/                      C/C++ MPI simulation runtime
    CMakeLists.txt            Build configuration for mpicc or mpicxx
  configs/                    Example graph and workload configurations
  experiments/                Scripts to reproduce runs and collect results
  outputs/                    Generated graphs, code, logs, and summaries
  REPORT.md                   Experiment writeup
```

## Running an end-to-end example

A typical workflow is shown below.

```
1) Generate and enrich a graph
   - run the NetGameSim-based generator
   - emit a single graph artifact, for example graph.json

2) Convert the enriched graph into an MPI program
   - generate code and a build directory

3) Compile
   - build with mpicc or mpicxx

4) Run
   - launch with mpirun using a rank count equal to the node count
```

Consider the following example.

```bash
# 1) Graph generation and enrichment
./tools/graph_export/run.sh configs/small.conf outputs/graph.json

# 2) Code generation
./tools/graph2mpi/run.sh outputs/graph.json outputs/mpi_program

# 3) Build
cd outputs/mpi_program
cmake -S . -B build
cmake --build build

# 4) Run
mpirun -n 64 ./build/ngs_mpi_sim --steps 20000 --seed 7
```

The example uses 64 ranks, so the input graph should contain 64 nodes.

## Configuration format

This project should use one configuration file per experiment. It should capture graph parameters, edge labeling parameters, and node PDF parameters. An example configuration is shown below.

```hocon
graph {
  nodes = 64
  seed = 100
  model = "erdos_renyi"
  edge_probability = 0.05
  require_connected = true
}

messages {
  types = [
    { name = "heartbeat", payload_bytes = 16 },
    { name = "control",   payload_bytes = 64 },
    { name = "data",      payload_bytes = 1024 }
  ]
}

edge_labels {
  strategy = "random_subset"
  min_types_per_edge = 1
  max_types_per_edge = 2
}

node_pdfs {
  strategy = "dirichlet"
  concentration = 0.7
  rate_model = { kind = "poisson", lambda_per_step = 0.3 }
}

runtime {
  steps = 20000
  compute_model = { kind = "busy_loop", mean_us = 50 }
  channel_backend = "edge_comm"
}
```

## Output artifacts

At the end of a run, the repository should contain the following artifacts.

* The enriched graph artifact in a portable format such as JSON.
* The generated MPI program sources, plus build files.
* Execution logs and a machine-readable summary of metrics such as sent count, received count, bytes sent, and per-type breakdowns.
* A short report describing what was implemented and what experiments were run.

## Deliverables

The required deliverables are listed below.

* A NetGameSim extension that exports enriched graphs with edge message-type labels and per-node PDFs.
* A graph-to-MPI converter that produces a compilable C or C++ MPI program, or a fixed runtime plus generated headers.
* An MPI runtime that executes the workload without deadlock and produces a metrics summary.
* At least 2 experiments that compare outcomes across different graph structures or different workload PDFs.
* Documentation that makes the results reproducible from configuration and scripts.

## Suggested experiments and extensions

This framework is meant to be a base for additional distributed algorithms and performance studies. A set of good next steps is listed below.

* Compare communicator-per-edge versus tag-based channels, then explain when communicator creation dominates runtime.
* Implement gossip-style dissemination, epidemic membership, or probabilistic broadcast, then measure convergence time across graph families.
* Add a distributed snapshot algorithm such as Chandy Lamport and validate it on graphs with cycles.
* Implement termination detection, for example Dijkstra Scholten, and compare message overhead under different PDFs.
* Add logical clocks, such as Lamport clocks or vector clocks, and use them to detect causal order violations in a deliberately adversarial workload.
* Add fault injection at the simulation layer, for example simulated message delay, drop, or process pauses, then evaluate algorithm robustness.

## Attribution and licensing

This project builds on NetGameSim by [Prof. Grechanik](http://www.cs.uic.edu/~drmark/) and follows the upstream license and attribution requirements. The upstream repository and the walkthrough video are listed near the top of this README.
