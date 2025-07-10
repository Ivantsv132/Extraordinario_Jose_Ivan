import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

public class ClienteMain {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    private static JTextArea areaTextoActual;
    private static DefaultListModel<String> historialModelo;
    private static JLabel imagenLabel;
    private static Socket socket;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClienteMain::crearGUI);
    }

    private static void crearGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Cliente Portapapeles Distribuido - Texto + Imagen");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 240, 240));

        areaTextoActual = new JTextArea(5, 40);
        areaTextoActual.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        areaTextoActual.setBackground(new Color(255, 255, 250));
        areaTextoActual.setBorder(BorderFactory.createTitledBorder("Texto Actual"));
        JScrollPane scrollTexto = new JScrollPane(areaTextoActual);

        historialModelo = new DefaultListModel<>();
        JList<String> listaHistorial = new JList<>(historialModelo);
        listaHistorial.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane scrollHistorial = new JScrollPane(listaHistorial);
        scrollHistorial.setPreferredSize(new Dimension(200, 0));
        scrollHistorial.setBorder(BorderFactory.createTitledBorder("Historial"));

        imagenLabel = new JLabel();
        imagenLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollImagen = new JScrollPane(imagenLabel);
        scrollImagen.setBorder(BorderFactory.createTitledBorder("Imagen Recibida"));

        JButton botonEnviarTexto = new JButton("Enviar Texto");
        JButton botonLeerTexto = new JButton("Leer Texto");
        JButton botonEnviarImagen = new JButton("Enviar Imagen");

        Font fuenteBoton = new Font("Segoe UI", Font.BOLD, 13);
        botonEnviarTexto.setFont(fuenteBoton);
        botonLeerTexto.setFont(fuenteBoton);
        botonEnviarImagen.setFont(fuenteBoton);

        Color azulClaro = new Color(200, 220, 255);
        Color verdeClaro = new Color(200, 255, 200);
        Color naranjaClaro = new Color(255, 230, 180);

        botonEnviarTexto.setBackground(azulClaro);
        botonEnviarTexto.setForeground(Color.BLACK);

        botonLeerTexto.setBackground(verdeClaro);
        botonLeerTexto.setForeground(Color.BLACK);

        botonEnviarImagen.setBackground(naranjaClaro);
        botonEnviarImagen.setForeground(Color.BLACK);

        botonEnviarTexto.setFocusPainted(false);
        botonLeerTexto.setFocusPainted(false);
        botonEnviarImagen.setFocusPainted(false);

        botonEnviarTexto.setMargin(new Insets(10, 20, 10, 20));
        botonLeerTexto.setMargin(new Insets(10, 20, 10, 20));
        botonEnviarImagen.setMargin(new Insets(10, 20, 10, 20));

        JPanel panelBotones = new JPanel(new GridLayout(1, 3, 10, 0));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelBotones.add(botonEnviarTexto);
        panelBotones.add(botonLeerTexto);
        panelBotones.add(botonEnviarImagen);

        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centro.add(scrollTexto);
        centro.add(scrollImagen);

        frame.add(scrollHistorial, BorderLayout.WEST);
        frame.add(centro, BorderLayout.CENTER);
        frame.add(panelBotones, BorderLayout.SOUTH);

        frame.setVisible(true);

        botonEnviarTexto.addActionListener(e -> enviarTexto());
        botonLeerTexto.addActionListener(e -> leerDesdePortapapeles());
        botonEnviarImagen.addActionListener(e -> leerYEnviarImagen());

        conectarServidor(); // Conecta socket al servidor
    }

    private static void conectarServidor() {
        try {
            socket = new Socket(HOST, PUERTO); // Crea conexión con servidor
            new Thread(ClienteMain::escucharServidor).start(); // Hilo para escuchar mensajes entrantes
        } catch (IOException e) {
            mostrarError("No se pudo conectar: " + e.getMessage());
        }
    }

    private static void escucharServidor() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream()); // Canal de entrada
            while (true) {
                String tipo = dis.readUTF(); // Lee tipo de dato
                if ("TEXT".equals(tipo)) {
                    String texto = dis.readUTF();
                    SwingUtilities.invokeLater(() -> { // Actualiza GUI en hilo Swing
                        areaTextoActual.setText(texto);
                        historialModelo.addElement("Texto recibido: " + texto);
                        escribirEnPortapapeles(texto); // Copia al portapapeles local
                        mostrarNotificacion("Texto recibido");
                    });
                } else if ("IMAGE".equals(tipo)) {
                    int length = dis.readInt(); // Tamaño de imagen
                    byte[] imgBytes = new byte[length]; // Array para imagen
                    dis.readFully(imgBytes); // Lee bytes de imagen
                    ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes); // Convierte bytes a InputStream
                    BufferedImage bimg = ImageIO.read(bais); // Lee imagen de InputStream
                    SwingUtilities.invokeLater(() -> {
                        imagenLabel.setIcon(new ImageIcon(bimg));
                        historialModelo.addElement("Imagen recibida");
                        mostrarNotificacion("Imagen recibida");
                    });
                }
            }
        } catch (IOException e) {
            mostrarError("Conexión cerrada: " + e.getMessage());
        }
    }

    private static void enviarTexto() {
        String texto = areaTextoActual.getText().trim();
        if (texto.isEmpty()) {
            mostrarError("No hay texto para enviar");
            return;
        }
        try {
            //abre un canal de escritura
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Canal de salida
            dos.writeUTF("TEXT");
            dos.writeUTF(texto); // Envía texto
            dos.flush();
            historialModelo.addElement("Yo: " + texto);
            escribirEnPortapapeles(texto); // Copia al portapapeles local
        } catch (IOException e) {
            mostrarError("Error enviando texto: " + e.getMessage());
        }
    }

    private static void leerDesdePortapapeles() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); // portapapeles
            Transferable contenido = clipboard.getContents(null); //  contenido
            if (contenido != null && contenido.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String texto = (String) contenido.getTransferData(DataFlavor.stringFlavor); // Obtiene texto
                areaTextoActual.setText(texto);
                mostrarNotificacion("Texto leído del portapapeles");
            } else {
                mostrarError("No hay texto en el portapapeles");
            }
        } catch (Exception e) {
            mostrarError("Error leyendo portapapeles: " + e.getMessage());
        }
    }

    private static void leerYEnviarImagen() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); // portapapeles
            Transferable contenido = clipboard.getContents(null); // actual
            if (contenido != null && contenido.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image img = (Image) contenido.getTransferData(DataFlavor.imageFlavor); // Obtiene imagen

                BufferedImage bimg = new BufferedImage(
                        img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB); // Buffer de imagen
                Graphics2D g2d = bimg.createGraphics(); // Crea contexto gráfico
                g2d.drawImage(img, 0, 0, null); // Dibuja imagen en buffer
                g2d.dispose(); // Libera recursos gráficos

                ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Salida de bytes
                ImageIO.write(bimg, "png", baos); // Escribe como PNG
                baos.flush(); // Limpia buffer
                byte[] imgBytes = baos.toByteArray(); // Array de bytes
                baos.close(); // Cierra flujo

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Canal de salida
                dos.writeUTF("IMAGE");
                dos.writeInt(imgBytes.length); // Envía tamaño
                dos.write(imgBytes); // Envía bytes de imagen
                dos.flush();

                historialModelo.addElement("Imagen enviada");
                mostrarNotificacion("Imagen enviada");

            } else {
                mostrarError("No hay imagen en el portapapeles");
            }
        } catch (Exception e) {
            mostrarError("Error enviando imagen: " + e.getMessage());
        }
    }

    private static void escribirEnPortapapeles(String texto) {
        StringSelection sel = new StringSelection(texto); // Crea objeto de texto seleccionable
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); // Obtiene portapapeles
        clipboard.setContents(sel, null); // Pone texto en portapapeles
    }

    private static void mostrarNotificacion(String msg) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
    }

    private static void mostrarError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE));
    }
}
