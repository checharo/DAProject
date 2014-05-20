package com.stockmarketer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import damulticast.Device;
import damulticast.Message;
import damulticast.RemoteDevice;
import damulticast.ResourceState;
import damulticast.Tracker;

public class MainActivity extends ActionBarActivity {
	// public double V1, V2;
	public EditText txtBox1, txtBox2, txtBox3;
	public String ipTracker;
	public TextView Resultado, PeerlistTxt, ResourcesTxt;
	public Device device = new Device();;
	public Button btn1_peerlist, btn7_join, btn12_myip, btn6_hello, btn5_send,
			btn4_exit, btn8_release, btn9_lock, btn10_resources, btn11_newitem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// esto le borre del manifest android:targetSdkVersion="14"

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// disable buttons

		txtBox1 = (EditText) findViewById(R.id.editText1);
		txtBox2 = (EditText) findViewById(R.id.editText2);
		txtBox3 = (EditText) findViewById(R.id.editText3);

		Resultado = (TextView) findViewById(R.id.textView1);
		PeerlistTxt = (TextView) findViewById(R.id.textView2);
		ResourcesTxt = (TextView) findViewById(R.id.textView3);

		btn1_peerlist = (Button) findViewById(R.id.button1);
		btn7_join = (Button) findViewById(R.id.button7);

		btn12_myip = (Button) findViewById(R.id.button12);

		btn6_hello = (Button) findViewById(R.id.button6);
		btn5_send = (Button) findViewById(R.id.button5);
		btn4_exit = (Button) findViewById(R.id.button4);
		btn8_release = (Button) findViewById(R.id.button8);
		btn9_lock = (Button) findViewById(R.id.button9);
		btn10_resources = (Button) findViewById(R.id.button10);
		btn11_newitem = (Button) findViewById(R.id.button11);

		// 1,7,3,2
		btn1_peerlist.setEnabled(true);
		btn7_join.setEnabled(true);

		// 12, 6, 5, 4

		btn12_myip.setEnabled(true);
		btn6_hello.setEnabled(false);
		btn5_send.setEnabled(false);
		btn4_exit.setEnabled(true);

		// 8, 9, 10, 11

		btn8_release.setEnabled(true);
		btn9_lock.setEnabled(true);
		btn10_resources.setEnabled(true);
		btn11_newitem.setEnabled(true);

		// end of disable buttons

		// txtBox1 = (EditText) findViewById(R.id.editText1);
		// txtBox2 = (EditText) findViewById(R.id.editText2);

		// ipTracker = txtBox1.getText().toString();

		Tracker tracker = new Tracker();
		Thread t = new Thread(tracker);
		t.start();

		Resultado.setText(t.getState().toString());

	}

	public void peerlist(View view) {
		PeerlistTxt.setText("");
		if (device.getPeers().isEmpty())
			PeerlistTxt.setText("Peerlist empty");
		else {

			for (int i = 0; i < (device.getPeers().size() - 1); i++) {

				PeerlistTxt.append(device.getPeers().get(i).getId() + " -> "
						+ device.getPeers().get(i).getIpAddress().toString()
						+ ":" + device.getPeers().get(i).getPort() + "\n");
			}

		}
	}

	public void join(View view) throws IOException {

		String serverIP = txtBox1.getText().toString();

		/* Establish the connection with tracker */
		try {
			device.establishConnection(serverIP);
			Resultado.setText("Connected to: " + serverIP);
			btn6_hello.setEnabled(true);
			btn5_send.setEnabled(true);

			if (device.getPeers().isEmpty())
				PeerlistTxt.setText("Peerlist empty");
			else {
				PeerlistTxt.setText("");

				for (int i = 0; i < (device.getPeers().size() - 1); i++) {
					PeerlistTxt.append(device.getPeers().get(i).getId()
							+ " -> "
							+ device.getPeers().get(i).getIpAddress()
									.toString() + ":"
							+ device.getPeers().get(i).getPort() + "\n");
				}
			}

		} catch (UnknownHostException uhe) {
			Resultado.setText("Unknown host: " + serverIP);
		} catch (IOException ioe) {
			Resultado.setText("IOException while establishing P2P: "
					+ ioe.getMessage());
		}
		btn7_join.setEnabled(false);
		/* Start listening to P2P communication */
		Thread t = new Thread(device);
		t.start();

		/* Say hello to rest of peers */
		device.sayHello();

	}

	public void hello(View view) {
		device.sayHello();
	}

	public void send(View view) {

		// not implemented yet creo que debo implemantr aqui un objeto de tipo
		// device y no utilizar el public declarado arriba
		String command = txtBox2.getText().toString();

		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		try {
			int peerId = Integer.parseInt(st.nextToken());
			String header = st.nextToken();
			String message = st.nextToken();

			RemoteDevice peer = device.lookUpPeer(peerId);
			if (peer == null) {
				System.err.println("No peer entry for " + peerId);
			}
			device.send(new Message(peer, header, message));
		} catch (NoSuchElementException nsee) {
			System.err.println("usage: send <peerId> <header> <message>");
		} catch (NumberFormatException nfe) {
			System.err.println("peer id must be numerical");
		}

	}

	public void exit(View view) {
		device.sayGoodbye();

		finish();
		System.exit(0);

	}

	public void newitem(View view) {

		String itemStr = txtBox2.getText().toString();
		int value = Integer.parseInt(txtBox3.getText().toString());

		try {
			// st = new StringTokenizer(st.nextToken(), "|");

			if (device.getSharedResources().hasValue(itemStr)) {
				Resultado.setText("The system already has a value for: "
						+ itemStr);
				System.err.println("The system already has a value for: "
						+ itemStr);
			} else {
				device.addNewResource(itemStr, value);

			}
		} catch (NoSuchElementException nsee) {
			Resultado.setText("usage: new <resource_name>|<value>" + itemStr);
			System.err.println("usage: new <resource_name>|<value>" + itemStr);
		} catch (NumberFormatException nfe) {
			Resultado.setText("value must be numerical");
			System.err.println("value must be numerical");
		}

	}

	public void resources(View view) {
		Resultado.setText("");

		HashMap<String, Integer> values = device.getSharedResources()
				.getValues();
		if (values.isEmpty()) {
			Resultado.setText("resource list empty");

			System.out.println("Resources list is empty.");
		} else
			for (String key : values.keySet()) {
				String entry = key + ":" + values.get(key);
				HashMap<String, ResourceState> locks = device
						.getSharedResources().getLocks();
				try {
					entry += ":" + locks.get(key).getState();
				} catch (NullPointerException npe) {
					entry += ":LOCK_NOT_INITIALIZED";
				}
				Resultado.append(entry + "\n");
				System.out.println(entry);
			}
	}

	public void lock(View view) {
		// txtBox1.setText("192.168.56.101");// borrar

		String itemStr = txtBox2.getText().toString();

		StringTokenizer st = new StringTokenizer(itemStr, " ");
		st.nextToken();
		try {
			String key = st.nextToken();
			if (device.getSharedResources().hasValue(key)) {
				device.lockResource(key);
			} else {
				Resultado.setText("Resource not found: " + itemStr);

				System.err.println("Resource not found: " + key);
			}
		} catch (NoSuchElementException nsee) {
			Resultado.setText("usage: lock <resource_name>|<value>");

			System.err.println("usage: lock <resource_name>|<value>");
		} catch (NullPointerException npe) {
			/* Resource was wanted or held already, or does not exist */
			Resultado.setText(npe.getMessage());

			System.err.println(npe.getMessage());
		}

	}

	public void release(View view) {

		String itemStr = txtBox2.getText().toString();

		StringTokenizer st = new StringTokenizer(itemStr, " ");
		st.nextToken();
		try {
			// String key = st.nextToken();
			if (device.getSharedResources().hasValue(itemStr)) {
				device.releaseResource(itemStr);
			} else {
				Resultado.setText("Resource not found: " + itemStr);
				System.err.println("Resource not found: " + itemStr);
			}
		} catch (NoSuchElementException nsee) {
			Resultado.setText("usage: release <resource_name>");
			System.err.println("usage: release <resource_name>");
		} catch (NullPointerException npe) {
			/* Resource was wanted or held already, or does not exist */
			Resultado.setText(npe.getMessage());
			System.err.println(npe.getMessage());
		}
	}

	public void myip(View view) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						Resultado.setText(inetAddress.getHostAddress()
								.toString());
					}
				}
			}
		} catch (Exception e) {
			Resultado.setText("Exception caught =" + e.getMessage());
		}

	}

}
