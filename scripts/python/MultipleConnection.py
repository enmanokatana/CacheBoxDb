import socket
import threading
import time

# Server address and port
HOST = "127.0.0.1"
PORT = 20029

# Number of concurrent connections to test
MAX_CONNECTIONS = 100000  # Adjust this based on your server's expected capacity

# Shared counter for successful connections
successful_connections = 0
lock = threading.Lock()

def client_thread(thread_id):
    global successful_connections
    try:
        with socket.create_connection((HOST, PORT), timeout=5) as sock:
            # Send a simple command to test the connection
            command = "*1\r\n$4\r\nPING\r\n"  # Example: PING command
            sock.sendall(command.encode())

            # Wait for the response
            response = sock.recv(1024).decode()
            if response.strip() == "+PONG":
                with lock:
                    successful_connections += 1
                print(f"Thread {thread_id}: Connection successful. Response: {response}")
            else:
                print(f"Thread {thread_id}: Unexpected response: {response}")
    except Exception as e:
        print(f"Thread {thread_id}: Connection failed. Error: {e}")

def test_server_connection_limits():
    threads = []
    for i in range(1, MAX_CONNECTIONS + 1):
        thread = threading.Thread(target=client_thread, args=(i,))
        threads.append(thread)
        thread.start()

        # Optionally, add a delay between starting threads to avoid overwhelming the server
        # time.sleep(0.01)  # Uncomment if needed

    # Wait for all threads to complete
    for thread in threads:
        thread.join()

    print(f"Test completed. {successful_connections} out of {MAX_CONNECTIONS} connections were successful.")

if __name__ == "__main__":
    test_server_connection_limits()