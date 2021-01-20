package com.example;

import com.osrslogger.OsrsLogger;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OsrsLogger.class);
		RuneLite.main(args);
	}
}