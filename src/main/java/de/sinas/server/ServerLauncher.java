package de.sinas.server;

import de.sinas.net.PROTOCOL;

public class ServerLauncher {

	public static void main(String[] args) {
		new AppServer(PROTOCOL.PORT, "C:\\Users\\Yoshi\\Documents\\GitHub\\SiNaS\\db\\test.db");
	}

}