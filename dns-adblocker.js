const dns = require('dns');
const dgram = require('dgram');

const AD_DOMAINS = [
    'doubleclick.net',
    'googleads.g.doubleclick.net',
    'pagead2.googlesyndication.com',
    'adservice.google.com',
    'adnxs.com',
    'admob.com',
    'ads.inmobi.com',
    'unityads.unity3d.com',
    'applovin.com',
    'adcolony.com',
    'vungle.com',
    'ironsrc.com',
    'analytics.google.com'
];

const UPSTREAM_DNS = '1.1.1.1';
const UPSTREAM_PORT = 53;
const SERVER_PORT = 53;

function isBlocked(domain) {
    return AD_DOMAINS.some(ad => domain === ad || domain.endsWith('.' + ad));
}

function parseDomain(msg) {
    let offset = 12;
    let parts = [];
    while (offset < msg.length && msg[offset] !== 0) {
        let len = msg[offset];
        offset++;
        parts.push(msg.toString('utf8', offset, offset + len));
        offset += len;
    }
    return parts.join('.');
}

function createBlockedResponse(req) {
    const res = Buffer.from(req);
    // Set response flag QR=1, RCODE=3 (NXDOMAIN - Name Error)
    res[2] = 0x81;
    res[3] = 0x83;
    return res;
}

const server = dgram.createSocket('udp4');

server.on('message', (msg, rinfo) => {
    try {
        const domain = parseDomain(msg);
        if (domain && isBlocked(domain)) {
            console.log(`[BLOCKED AD] -> ${domain} (Client: ${rinfo.address})`);
            const blockedPacket = createBlockedResponse(msg);
            server.send(blockedPacket, rinfo.port, rinfo.address);
        } else {
            // Forward legitimate query to upstream DNS
            const client = dgram.createSocket('udp4');
            client.send(msg, UPSTREAM_PORT, UPSTREAM_DNS, (err) => {
                if (err) client.close();
            });
            client.on('message', (resMsg) => {
                server.send(resMsg, rinfo.port, rinfo.address);
                client.close();
            });
            client.on('error', () => client.close());
        }
    } catch (e) {
        console.error('Error handling packet:', e.message);
    }
});

server.on('listening', () => {
    const address = server.address();
    console.log(`Ad Blocker DNS server running on ${address.address}:${address.port}`);
    console.log(`Point your PC or Mobile Wi-Fi DNS to this machine's IP to block ads.`);
});

server.bind(SERVER_PORT);
