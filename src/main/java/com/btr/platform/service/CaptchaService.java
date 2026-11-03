package com.btr.platform.service;

import com.btr.platform.common.BizException;
import com.btr.platform.common.CacheService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final CacheService cacheService;

    public CaptchaService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public Captcha generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String key = java.util.UUID.randomUUID().toString();
        cacheService.set("captcha:" + key, code.toString(), Duration.ofMinutes(5));
        return new Captcha(key, "data:image/png;base64," + render(code.toString()), true);
    }

    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        String cached = cacheService.get("captcha:" + key);
        cacheService.delete("captcha:" + key);
        return cached != null && cached.equalsIgnoreCase(code.trim());
    }

    private String render(String code) {
        int width = 120;
        int height = 42;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(247, 248, 250));
        g.fillRect(0, 0, width, height);

        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(120 + random.nextInt(90), 120 + random.nextInt(90), 120 + random.nextInt(90)));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        g.setFont(new Font("Arial", Font.BOLD, 26));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30 + random.nextInt(70), 30 + random.nextInt(70), 30 + random.nextInt(70)));
            int y = 29 + random.nextInt(5);
            g.drawString(String.valueOf(code.charAt(i)), 16 + i * 25, y);
        }
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BizException(500, "验证码生成失败");
        }
    }

    public record Captcha(String key, String image, boolean enabled) {
    }
}
