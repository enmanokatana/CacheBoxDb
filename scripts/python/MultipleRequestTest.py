import socket

def send_multiple_requests():
    # Server address and port
    host = "127.0.0.1"
    port = 20029

    # Create RESP commands for multiple requests
    commands = [
        ["PUT", "string", "myKey1", "myValue1"],
        ["PUT", "string", "myKey2", "myValue2"],
        ["GET", "myKey1"],
        ["GET", "myKey2"],
        ["DELETE", "myKey1"],
        ["DELETE", "myKey2"]
    ]

    # Function to build RESP command
    def build_resp_command(command):
        resp_command = f"*{len(command)}\r\n"
        for item in command:
            resp_command += f"${len(item)}\r\n{item}\r\n"
        return resp_command

    try:
        # Connect to the server
        with socket.create_connection((host, port)) as sock:
            # Send commands and read responses sequentially
            for command in commands:
                resp_command = build_resp_command(command)
                sock.sendall(resp_command.encode())
                print(f"Sent command: {command}")

                # Wait for and print the response
                response = sock.recv(1024).decode()
                print("Response from server:", response)

    except ConnectionRefusedError:
        print("Error: Unable to connect to the server. Ensure the server is running.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    send_multiple_requests()