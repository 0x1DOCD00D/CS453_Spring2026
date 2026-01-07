#include <mpi.h>
#include <iostream>
#include <cstring>

#define PING_PONG_LIMIT 10

int main(int argc, char** argv) {
    // Initialize the MPI environment
    MPI_Init(&argc, &argv);

    // Get the number of processes
    int world_size;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);

    // Get the rank of the process
    int world_rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

    // We need exactly 2 processes for ping-pong
    if (world_size != 2) {
        if (world_rank == 0) {
            std::cerr << "Error: This application requires exactly 2 MPI processes." << std::endl;
            std::cerr << "Usage: mpirun -np 2 ./ping_pong" << std::endl;
        }
        MPI_Finalize();
        return 1;
    }

    int ping_pong_count = 0;
    int partner_rank = (world_rank + 1) % 2;
    
    while (ping_pong_count < PING_PONG_LIMIT) {
        if (world_rank == ping_pong_count % 2) {
            // Increment the ping pong count before sending
            ping_pong_count++;
            
            // Send the count to the partner
            MPI_Send(&ping_pong_count, 1, MPI_INT, partner_rank, 0, MPI_COMM_WORLD);
            
            std::cout << "Process " << world_rank << " sent ping_pong_count " 
                      << ping_pong_count << " to process " << partner_rank << std::endl;
        } else {
            // Receive the count from the partner
            MPI_Recv(&ping_pong_count, 1, MPI_INT, partner_rank, 0, 
                     MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            
            std::cout << "Process " << world_rank << " received ping_pong_count " 
                      << ping_pong_count << " from process " << partner_rank << std::endl;
        }
    }

    std::cout << "Process " << world_rank << " finished ping-pong with count " 
              << ping_pong_count << std::endl;

    // Finalize the MPI environment
    MPI_Finalize();
    return 0;
}
