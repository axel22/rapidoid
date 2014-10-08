package com.rapidoid.http;

/*
 * #%L
 * rapidoid-http
 * %%
 * Copyright (C) 2014 Nikolche Mihajlovski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.rapidoid.buffer.Buf;
import org.rapidoid.buffer.BufGroup;
import org.rapidoid.bytes.BYTES;
import org.rapidoid.data.Range;
import org.rapidoid.data.Ranges;
import org.rapidoid.net.TCP;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.Protocol;
import org.rapidoid.net.impl.RapidoidHelper;
import org.rapidoid.util.U;
import org.rapidoid.wrap.Bool;
import org.rapidoid.wrap.Int;

public class MaxHttpPerfTest {

	protected static final byte[] PREFIX = "Connection:".getBytes();

	protected static final byte[] RESP = "HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Length: 1\r\n\r\nX"
			.getBytes();

	public static void main(String[] args) {
		U.args(args);

		String req = "GET /plaintext HTTP/1.1\r\nHost:www.test.com\r\n\r\n";

		BufGroup gr = new BufGroup(14);
		final Buf buf = gr.newBuf();
		buf.append(req);

		final RapidoidHelper helper = new RapidoidHelper();

		final HttpParser parser = new HttpParser();

		for (int i = 0; i < 10; i++) {
			U.benchmark("HTTP parse", 3000000, new Runnable() {
				public void run() {
					buf.position(0);

					Range[] ranges = helper.ranges1.ranges;
					Ranges headers = helper.ranges2;

					Bool isGet = helper.booleans[0];
					Bool isKeepAlive = helper.booleans[1];

					Range verb = ranges[ranges.length - 1];
					Range uri = ranges[ranges.length - 2];
					Range path = ranges[ranges.length - 3];
					Range query = ranges[ranges.length - 4];
					Range protocol = ranges[ranges.length - 5];
					Range body = ranges[ranges.length - 6];

					parser.parse(buf, isGet, isKeepAlive, body, verb, uri, path, query, protocol, headers, helper);
				}
			});
		}

		TCP.listen(new Protocol() {
			@Override
			public void process(Channel ctx) {
				if (ctx.isInitial()) {
					return;
				}

				Ranges lines = ctx.helper().ranges1;
				lines.count = 0;

				Buf in = ctx.input();

				Int pos = ctx.helper().integers[0];

				int poss = BYTES.parseLines(in.bytes(), lines, pos, in.position(), in.size(), (byte) 'v', (byte) 'e');

				in.position(poss);

				ctx.write(RESP).done();
			}
		});
	}

}
