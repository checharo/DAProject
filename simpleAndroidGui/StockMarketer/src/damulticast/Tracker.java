package damulticast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Class that implements the tracker of the P2P network. The tracker's sole
 * purpose is to initialize the peer list in every device joining the P2P
 * network. Once this is done, it closes the connection. This is the only
 * process that will listen on a fixed port (12345), the rest of the ports will
 * be ephemeral.
 * 
 * @author cesar
 */
public class Tracker implements Runnable {

	/** Stores the list of peers in the network */
	private ArrayList<RemoteDevice> peers;

	/**
	 * Listening thread for new peers.
	 */
	public void run() {
		try {
			this.listen();
		} catch (IOException ioe) {
			System.err.println("General IOException: " + ioe);
		}
	}

	public Tracker() {
		this.peers = new ArrayList<RemoteDevice>();
	}

	/**
	 * The listen method for the servicing new peers.
	 * 
	 * @throws IOException
	 *             In case an unexpected error happens while reading connections
	 */
	public void listen() throws IOException {
		int serverPort = 12345;
		int id = 0;
		ServerSocket listenSocket = new ServerSocket(serverPort);
		while (true) {
			Socket clientSocket = listenSocket.accept();
			clientSocket.setSoTimeout(5000);
			DataInputStream in = new DataInputStream(
					clientSocket.getInputStream());
			DataOutputStream out = new DataOutputStream(
					clientSocket.getOutputStream());

			try {
				String ipAddress = clientSocket.getInetAddress()
						.getHostAddress();

				/* Ask peer which port will it use for the P2P communication */
				int assignedPort = in.readInt();
				/* Assign an id to peer, send it and close connection */
				id++;
				out.writeInt(id);
				RemoteDevice newClient = new RemoteDevice(id, ipAddress,
						assignedPort);

				/* Send the peer list */
				for (RemoteDevice peer : getPeers()) {
					out.writeBoolean(true);
					out.writeInt(peer.getId());
					out.writeUTF(peer.getIpAddress());
					out.writeInt(peer.getPort());
				}
				out.writeBoolean(false);

				/* Add new peer to peer list */
				getPeers().add(newClient);
			} catch (SocketTimeoutException ste) {
				System.err.println("Socket timeout from peer: "
						+ clientSocket.getInetAddress().getHostAddress());
			} catch (IOException ioe) {
				System.err.println("IOE exception while initializing peer: "
						+ clientSocket.getInetAddress().getHostAddress());
			} finally {

				clientSocket.close();
			}
		}
	}

	/**
	 * @return the peers
	 */
	public ArrayList<RemoteDevice> getPeers() {
		return peers;
	}

}
