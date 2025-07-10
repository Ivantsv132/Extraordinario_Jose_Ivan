import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorMain {
    private static final int PUERTO = 5000;
    private static List<Socket> clientes = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(PUERTO);
        System.out.println("Servidor iniciado en el puerto " + PUERTO);

        while (true) {

            Socket cliente = servidor.accept();
            // Agrega el nuevo cliente a la lista de clientes conectados
            clientes.add(cliente);
            System.out.println("Nuevo cliente conectado.");

            // Crea un nuevo hilo para manejar la comunicación con este cliente
            new Thread(() -> manejarCliente(cliente)).start();
        }
    }

    // maneja la comunicación con un cliente
    private static void manejarCliente(Socket cliente) {
        try {
            // Crea un canal de entrada para leer datos del cliente
            DataInputStream dis = new DataInputStream(cliente.getInputStream());

            while (true) {
                // tipo de mensaje enviado
                String tipo = dis.readUTF();

                if ("TEXT".equals(tipo)) {
                    String texto = dis.readUTF();
                    System.out.println("Recibido texto: " + texto);
                    propagarATodos(cliente, "TEXT", texto.getBytes("UTF-8"));

                } else if ("IMAGE".equals(tipo)) {
                    //tamaño de la imagen (número de bytes)
                    int length = dis.readInt();
                    byte[] imageBytes = new byte[length];

                    // lee todos los bytes de la imagen
                    dis.readFully(imageBytes);
                    System.out.println("Recibida imagen de " + length + " bytes");

                    propagarATodos(cliente, "IMAGE", imageBytes);
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        }
    }

    // enviar un mensaje o imagen a todos los clientes
    private static void propagarATodos(Socket origen, String tipo, byte[] data) {
        for (Socket cliente : clientes) {
            if (cliente != origen) {
                try {
                    // Crea un canal de salida
                    DataOutputStream dos = new DataOutputStream(cliente.getOutputStream());

                    // Envía primero el tipo de dato
                    dos.writeUTF(tipo);

                    if ("TEXT".equals(tipo)) {
                        //convierte bytes a cadena UTF-8 y la envía
                        dos.writeUTF(new String(data, "UTF-8"));
                    } else if ("IMAGE".equals(tipo)) {
                        // envía primero el tamaño y luego los bytes
                        dos.writeInt(data.length);
                        dos.write(data);
                    }
                    dos.flush();
                } catch (IOException e) {
                    System.out.println("Error al propagar: " + e.getMessage());
                }
            }
        }
    }
}
