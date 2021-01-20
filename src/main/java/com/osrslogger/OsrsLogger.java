/*
 * Copyright (c) 2021, 0anth <https://github.com/0anth/osrs-logger>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.osrslogger;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "OSRS Loot Logger",
	description = "ALPHA plugin. Requires auth to use."
)
public class OsrsLogger extends Plugin
{
	@Inject
	private OsrsLoggerConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Client client;

	@Provides
	OsrsLoggerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OsrsLoggerConfig.class);
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		NPC npc = npcLootReceived.getNpc();
		Collection<ItemStack> items = npcLootReceived.getItems();

		processLoot(npc.getId(), items);
	}

	private void processLoot(int npcId, Collection<ItemStack> items)
	{
		if (Strings.isNullOrEmpty(config.authCode()))
		{
			return;
		}

		boolean once = false;
		Player player = client.getLocalPlayer();

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{ ");

		stringBuilder.append("\"player\" : ");
		stringBuilder.append("\"").append(player.getName()).append("\", ");

		stringBuilder.append("\"enemy\" : ");
		stringBuilder.append("\"").append(npcId).append("\", ");

		stringBuilder.append("\"authCode\" : ");
		stringBuilder.append("\"").append(config.authCode()).append("\", ");

		stringBuilder.append("\"items\" : ");
		stringBuilder.append("{ ");

		for (ItemStack item : stack(items))
		{
			if (once)
			{
				stringBuilder.append(", ");
			}
			else
			{
				once = true;
			}

			int itemId = item.getId();
			int qty = item.getQuantity();

			stringBuilder.append("\"").append(itemId).append("\" : ");
			stringBuilder.append("\"").append(qty).append("\"");
		}

		stringBuilder.append(" } }");

		String compiledJson = stringBuilder.toString();

		sendWebhook(compiledJson);
	}

	private void sendWebhook(String compiledJson)
	{
		String configUrl = "https://osrs.anthd.com/?add";

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("json", compiledJson);

		buildRequestAndSend(url, requestBodyBuilder);
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
			.url(url)
			.post(requestBody)
			.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}

	private static Collection<ItemStack> stack(Collection<ItemStack> items)
	{
		final List<ItemStack> list = new ArrayList<>();

		for (final ItemStack item : items)
		{
			int quantity = 0;
			for (final ItemStack i : list)
			{
				if (i.getId() == item.getId())
				{
					quantity = i.getQuantity();
					list.remove(i);
					break;
				}
			}
			if (quantity > 0)
			{
				list.add(new ItemStack(item.getId(), item.getQuantity() + quantity, item.getLocation()));
			}
			else
			{
				list.add(item);
			}
		}

		return list;
	}
}
