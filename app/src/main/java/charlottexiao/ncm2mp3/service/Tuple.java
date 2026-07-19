package charlottexiao.ncm2mp3.service;

/**
 * 转码结果 (成功, 格式)
 *
 * @param result 转码是否成功
 * @param format 转码的输出格式
 * @author minokori
 */
public record Tuple(Boolean result, String format) {
}
