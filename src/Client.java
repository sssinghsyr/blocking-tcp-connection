package multithreadedClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Client implements Runnable{
	private Socket socket;

	public Client(String serverAddress, int serverPort) throws IOException {
		this.socket = new Socket(serverAddress, serverPort);
	}

	@Override
	public void run() {
		while (true) {
			DataInputStream input;
			DataOutputStream output;
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
				output.writeBytes("Hello\n");
				int character;
				StringBuilder data = new StringBuilder();
				while ((character = input.read()) < 1) {
					data.append((char) character);
				}
				//System.out.println(data.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		List<Client> lists = new ArrayList<Client>();
		for(int i=0; i<100; i++) {
			Client cli = new Client("localhost",19000);
			lists.add(cli);
		}
		for(Client it: lists) {
			Thread clientThread = new Thread(it);
			clientThread.start();
		}
	}

}
