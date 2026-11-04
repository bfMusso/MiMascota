package uy.mimascota.fotos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uy.mimascota.comun.ApiException;

/**
 * Guarda las fotos de las mascotas: acepta JPG, PNG o WebP, las achica a 800 px de lado
 * como máximo y las guarda en JPG con un nombre aleatorio. Se sirven en /fotos/{archivo}.
 */
@Service
public class FotoService {

    private static final Logger log = LoggerFactory.getLogger(FotoService.class);
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int LADO_MAXIMO = 800;

    private final Path carpeta;

    public FotoService(FotosProperties props) {
        this.carpeta = Path.of(props.carpeta()).toAbsolutePath().normalize();
    }

    public Path carpeta() {
        return carpeta;
    }

    /** Devuelve el nombre del archivo guardado. */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || !TIPOS.contains(archivo.getContentType())) {
            throw fotoInvalida();
        }
        BufferedImage original;
        try (InputStream in = archivo.getInputStream()) {
            original = ImageIO.read(in);
        } catch (IOException e) {
            throw fotoInvalida();
        }
        if (original == null) {
            throw fotoInvalida();
        }

        String nombre = UUID.randomUUID() + ".jpg";
        try {
            Files.createDirectories(carpeta);
            escribirJpg(achicar(original), carpeta.resolve(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
        return nombre;
    }

    public void borrar(String nombre) {
        if (nombre == null) {
            return;
        }
        Path archivo = carpeta.resolve(nombre).normalize();
        if (!archivo.startsWith(carpeta)) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar la foto {}", archivo, e);
        }
    }

    private static BufferedImage achicar(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * escala));
        int nh = Math.max(1, (int) Math.round(h * escala));
        // Siempre se redibuja en RGB: JPG no tiene transparencia, así un PNG transparente queda con fondo blanco.
        BufferedImage destino = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return destino;
    }

    private static void escribirJpg(BufferedImage img, Path destino) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(destino.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static ApiException fotoInvalida() {
        return new ApiException(HttpStatus.BAD_REQUEST, "foto_invalida", "La foto tiene que ser JPG, PNG o WebP");
    }
}
