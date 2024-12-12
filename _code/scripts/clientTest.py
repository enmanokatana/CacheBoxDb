import socket

def send_put_and_get_request():
    # Server address and port
    host = "127.0.0.1"
    port = 20029

    # Create a RESP PUT command
    put_command = ["PUT", "string", "myKey", "myValue"]
    resp_put_command = f"*{len(put_command)}\r\n"
    for item in put_command:
        resp_put_command += f"${len(item)}\r\n{item}\r\n"

    # Create a RESP GET command
    get_command = ["GET", "myKey"]
    resp_get_command = f"*{len(get_command)}\r\n"
    for item in get_command:
        resp_get_command += f"${len(item)}\r\n{item}\r\n"

    try:
        # Connect to the server
        with socket.create_connection((host, port)) as sock:
            # Send the PUT command
            sock.sendall(resp_put_command.encode())

            # Receive and print the server's response for PUT
            put_response = sock.recv(1024).decode()
            print("Response from server (PUT):", put_response)

            # If PUT was successful, send the GET command
            if put_response.startswith("+OK"):
                sock.sendall(resp_get_command.encode())

                # Receive and print the server's response for GET
                get_response = sock.recv(1024).decode()
                print("Response from server (GET):", get_response)

    except ConnectionRefusedError:
        print("Error: Unable to connect to the server. Ensure the server is running.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    send_put_and_get_request()
