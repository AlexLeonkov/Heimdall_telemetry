package de.tomcory.heimdall.scanner.traffic.mitm.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import timber.log.Timber;

/**
 * An SSL/TLS server, that will listen to a specific address and port and serve SSL/TLS connections
 * compatible with the protocol it applies.
 * <p/>
 * </p>
 * NioSslServer makes use of Java NIO, and specifically listens to new connection requests with a {@link ServerSocketChannel}, which will
 * create new {@link SocketChannel}s and a {@link Selector} which serves all the connections in one thread.
 *
 * @author <a href="mailto:alex.a.karnezis@gmail.com">Alex Karnezis</a>
 */
public class NioSslServer /*extends NioSslPeer*/ {
//
//    /**
//     * Declares if the server is active to serve and create new connections.
//     */
//    private boolean active;
//
//    /**
//     * The engine that will be used to encrypt/decrypt data between this client and the server.
//     */
//    private final SSLEngine engine;
//
//    /**
//     * A part of Java NIO that will be used to serve all connections to the server in one thread.
//     */
//    private final Selector selector;
//
//
//    /**
//     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
//     *
//     * @param hostAddress - the IP address this server will listen to.
//     * @param port - the port this server will listen to.
//     */
//    public NioSslServer(String hostAddress, int port, SSLEngine engine) throws Exception {
//        super(engine);
//        this.engine = engine;
//
//        SSLSession session = engine.getSession();
//        myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
//        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
//        peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
//        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
//
//        selector = SelectorProvider.provider().openSelector();
//        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//        serverSocketChannel.configureBlocking(false);
//        serverSocketChannel.socket().bind(new InetSocketAddress(hostAddress, port));
//        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//
//        active = true;
//
//    }
//
//    /**
//     * Should be called in order the server to start listening to new connections.
//     * This method will run in a loop as long as the server is active. In order to stop the server
//     * you should use {@link NioSslServer#stop()} which will set it to inactive state
//     * and also wake up the listener, which may be in blocking select() state.
//     */
//    public void start() throws Exception {
//
//        Timber.d("Initialized and waiting for new connections...");
//
//        while (isActive()) {
//            selector.select();
//            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
//            while (selectedKeys.hasNext()) {
//                SelectionKey key = selectedKeys.next();
//                selectedKeys.remove();
//                if (!key.isValid()) {
//                    continue;
//                }
//                if (key.isAcceptable()) {
//                    accept(key);
//                } else if (key.isReadable()) {
//                    read((SocketChannel) key.channel(), (SSLEngine) key.attachment());
//                }
//            }
//        }
//
//        Timber.d("Goodbye!");
//
//    }
//
//    /**
//     * Sets the server to an inactive state, in order to exit the reading loop in {@link NioSslServer#start()}
//     * and also wakes up the selector, which may be in select() blocking state.
//     */
//    public void stop() {
//        Timber.d("Will now close server...");
//        active = false;
//        executor.shutdown();
//        selector.wakeup();
//    }
//
//    /**
//     * Will be called after a new connection request arrives to the server. Creates the {@link SocketChannel} that will
//     * be used as the network layer link, and the {@link SSLEngine} that will encrypt and decrypt all the data
//     * that will be exchanged during the session with this specific client.
//     *
//     * @param key - the key dedicated to the {@link ServerSocketChannel} used by the server to listen to new connection requests.
//     */
//    private void accept(SelectionKey key) throws Exception {
//
//        Timber.d("New connection request!");
//
//        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
//        socketChannel.configureBlocking(false);
//
//        engine.setUseClientMode(false);
//        engine.beginHandshake();
//
//        if (doHandshake(socketChannel)) {
//            socketChannel.register(selector, SelectionKey.OP_READ, engine);
//        } else {
//            socketChannel.close();
//            Timber.d("Connection closed due to handshake failure.");
//        }
//    }
//
//    /**
//     * Will be called by the selector when the specific socket channel has data to be read.
//     * As soon as the server reads these data, it will call {@link NioSslServer#write(SocketChannel, SSLEngine, String)}
//     * to send back a trivial response.
//     *
//     * @param socketChannel - the transport link used between the two peers.
//     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
//     * @throws IOException if an I/O error occurs to the socket channel.
//     */
//    @Override
//    protected void read(SocketChannel socketChannel, SSLEngine engine) throws IOException {
//
//        Timber.d("About to read from a client...");
//
//        peerNetData.clear();
//        int bytesRead = socketChannel.read(peerNetData);
//        if (bytesRead > 0) {
//            peerNetData.flip();
//            while (peerNetData.hasRemaining()) {
//                peerAppData.clear();
//                SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
//                switch (result.getStatus()) {
//                    case OK:
//                        peerAppData.flip();
//                        Timber.d("Incoming message: %s", new String(peerAppData.array()));
//                        break;
//                    case BUFFER_OVERFLOW:
//                        peerAppData = enlargeApplicationBuffer(engine, peerAppData);
//                        break;
//                    case BUFFER_UNDERFLOW:
//                        peerNetData = handleBufferUnderflow(engine, peerNetData);
//                        break;
//                    case CLOSED:
//                        Timber.d("Client wants to close connection...");
//                        closeConnection(socketChannel);
//                        Timber.d("Goodbye client!");
//                        return;
//                    default:
//                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
//                }
//            }
//
//            write(socketChannel, engine, "Hello! I am your server!");
//
//        } else if (bytesRead < 0) {
//            Timber.e("Received end of stream. Will try to close connection with client...");
//            handleEndOfStream(socketChannel);
//            Timber.d("Goodbye client!");
//        }
//    }
//
//    /**
//     * Will send a message back to a client.
//     *
//     * @param socketChannel -  the socket channel that will be used to write to the client.
//     * @param message - the message to be sent.
//     * @throws IOException if an I/O error occurs to the socket channel.
//     */
//    @Override
//    protected void write(SocketChannel socketChannel, SSLEngine engine, String message) throws IOException {
//
//        Timber.d("About to write to a client...");
//
//        myAppData.clear();
//        myAppData.put(message.getBytes());
//        myAppData.flip();
//        while (myAppData.hasRemaining()) {
//            // The loop has a meaning for (outgoing) messages larger than 16KB.
//            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
//            myNetData.clear();
//            SSLEngineResult result = engine.wrap(myAppData, myNetData);
//            switch (result.getStatus()) {
//                case OK:
//                    myNetData.flip();
//                    while (myNetData.hasRemaining()) {
//                        socketChannel.write(myNetData);
//                    }
//                    Timber.d("Message sent to the client: %s", message);
//                    break;
//                case BUFFER_OVERFLOW:
//                    myNetData = enlargePacketBuffer(engine, myNetData);
//                    break;
//                case BUFFER_UNDERFLOW:
//                    throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
//                case CLOSED:
//                    closeConnection(socketChannel);
//                    return;
//                default:
//                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
//            }
//        }
//    }
//
//    /**
//     * Determines if the the server is active or not.
//     *
//     * @return if the server is active or not.
//     */
//    private boolean isActive() {
//        return active;
//    }

}