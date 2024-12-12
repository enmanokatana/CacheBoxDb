import socket
import threading

def handle_client_commands(commands, host, port):
    # Function to build RESP command
    def build_resp_command(command):
        resp_command = f"*{len(command)}\r\n"
        for item in command:
            resp_command += f"${len(item)}\r\n{item}\r\n"
        return resp_command

    try:
        # Connect to the server
        with socket.create_connection((host, port)) as sock:
            # Send all commands in sequence
            for command in commands:
                resp_command = build_resp_command(command)
                sock.sendall(resp_command.encode())
                print(f"Sent command: {command}")

            # Read and print all responses sequentially
            for _ in commands:
                response = sock.recv(1024).decode()
                print("Response from server:", response)

    except ConnectionRefusedError:
        print("Error: Unable to connect to the server. Ensure the server is running.")
    except Exception as e:
        print(f"An error occurred: {e}")

def test_multiple_connections():
    # Server address and port
    host = "127.0.0.1"
    port = 20029

    # Define commands for different clients
    client_commands = [
        [
            ["PUT", "string", "client1Key1", "client1Value1"],
            ["GET", "client1Key1"],
            ["DELETE", "client1Key1"]
        ],
        [
            ["PUT", "string", "client2Key1", "client2Value1"],
            ["GET", "client2Key1"],
            ["DELETE", "client2Key1"]
        ],
        [
            ["PUT", "string", "client3Key1", "client3Value1"],
            ["GET", "client3Key1"],
            ["DELETE", "client3Key1"]
        ]
    ]

    # Create and start threads for each client
    threads = []
    for commands in client_commands:
        thread = threading.Thread(target=handle_client_commands, args=(commands, host, port))
        threads.append(thread)
        thread.start()

    # Wait for all threads to finish
    for thread in threads:
        thread.join()

if __name__ == "__main__":
    test_multiple_connections()
lt