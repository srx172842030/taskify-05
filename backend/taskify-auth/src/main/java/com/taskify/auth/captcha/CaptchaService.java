package com.taskify.auth.captcha;

import com.taskify.common.base.UuidV7;
import com.taskify.common.cache.RedisKeys;
import com.taskify.common.error.BusinessException;
import com.taskify.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * 图形验证码：生成 captchaId + base64 图片；答案存储于 Redis（TTL 5 分钟，一次性消费）。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final long TTL_MINUTES = 5;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedissonClient redissonClient;

    /** 测试环境后门：设为 false 时跳过校验。 */
    @Value("${taskify.captcha.enabled:true}")
    private boolean captchaEnabled;

    public CaptchaResponse generate() {
        String id = UuidV7.randomUuid();
        String code = randomCode(4);
        redissonClient.getBucket(RedisKeys.captcha(id)).set(code, Duration.ofMinutes(TTL_MINUTES));
        return new CaptchaResponse(id, toDataUrl(render(code)));
    }

    /** 校验验证码：不区分大小写，一次性消费；失败抛 PARAM_ERROR。 */
    public void validate(String captchaId, String captchaCode) {
        if (!captchaEnabled) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(RedisKeys.captcha(captchaId));
        String code = bucket.getAndDelete();
        if (code == null || !code.equalsIgnoreCase(captchaCode)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "验证码错误或已过期");
        }
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage render(String code) {
        int w = 120, h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.setColor(new Color(30, 30, 30));
        g.drawString(code, 16, 29);
        g.dispose();
        return img;
    }

    private String toDataUrl(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    public record CaptchaResponse(String captchaId, String image) {
    }
}
