package com.mgmt.module.auth;

import com.mgmt.common.I18n;
import com.mgmt.common.BizException;
import com.mgmt.module.auth.dto.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
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
import java.util.UUID;

/**
 * 图形验证码：答案只存 Redis，浏览器只拿到图片。
 * 校验后立即删除，保证一次一用，防止同一张验证码被重复提交。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "mgmt:auth:captcha:";
    private static final Duration TTL = Duration.ofMinutes(2);
    /** 去掉 0/O、1/I 等易混淆字符 */
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public CaptchaVO generate() {
        String code = randomCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + key, code, TTL);
        return new CaptchaVO(key, "data:image/png;base64," + render(code));
    }

    public void verify(String key, String input) {
        if (key == null || key.isBlank()) {
            throw new BizException(400, I18n.get("auth.captchaRequired"));
        }
        String expected = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        if (expected == null) {
            throw new BizException(400, I18n.get("auth.captchaExpired"));
        }
        // 无论对错都删除：避免攻击者用同一张图反复试
        redisTemplate.delete(KEY_PREFIX + key);
        if (input == null || !expected.equalsIgnoreCase(input.trim())) {
            throw new BizException(400, I18n.get("auth.captchaIncorrect"));
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线，只做浅色，避免影响识别
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 200 + random.nextInt(55)));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            int x = 14;
            for (char c : code.toCharArray()) {
                g.setColor(new Color(20 + random.nextInt(90), 30 + random.nextInt(90), 60 + random.nextInt(120)));
                double angle = (random.nextDouble() - 0.5) * 0.5;
                g.rotate(angle, x, HEIGHT / 2.0);
                g.drawString(String.valueOf(c), x, 30);
                g.rotate(-angle, x, HEIGHT / 2.0);
                x += 26;
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BizException(500, I18n.get("auth.captchaGenerateFailed"));
        }
    }
}
