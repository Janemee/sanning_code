package com.huimi.common.tools;

import java.math.BigDecimal;

/**
 * 工具类-BigDecimal算法
 */
public class BigDecimalUtil {

    private static int DEF_DIV_SCALE = 10; // 默认精确的小数位

    /**
     * 提供精确的加法运算。
     *
     * @param params 参数数组
     * @return 和
     */
    public static double add(double... params) {
        BigDecimal b1 = new BigDecimal(0);
        for (double param : params) {
            BigDecimal b2 = new BigDecimal(Double.toString(param));
            b1 = b1.add(b2);
        }
        return b1.doubleValue();
    }

    /**
     * 提供精确的减法运算。
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 两个参数的差
     */
    public static double sub(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2).doubleValue();
    }

    /**
     * 提供精确的减法运算。
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 两个参数的差
     */
    public static double sub(BigDecimal v1, double v2) {
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return v1.subtract(b2).doubleValue();
    }

    /**
     * 提供精确的减法运算。
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 两个参数的差
     */
    public static double sub(double v1, BigDecimal v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        return b1.subtract(v2).doubleValue();
    }

    /**
     * 提供精确的乘法运算。
     *
     * @param params 参数数组
     * @return 动态参数的积
     */
    public static double mul(double... params) {
        BigDecimal b1 = new BigDecimal(1);
        for (double param : params) {
            BigDecimal b2 = new BigDecimal(Double.toString(param));
            b1 = b1.multiply(b2);
        }
        return b1.doubleValue();
    }

    /**
     * 获得负数
     *
     * @param b1 参数
     * @return -b1
     */
    public static double neg(BigDecimal b1) {
        return BigDecimal.ZERO.subtract(b1).doubleValue();
    }

    /**
     * 提供精确的乘法运算。
     *
     * @param params 参数数组
     * @return 动态参数的积
     */
    public static BigDecimal mul(BigDecimal... params) {
        BigDecimal b1 = new BigDecimal(1);
        for (BigDecimal param : params) {
            b1 = b1.multiply(param);
        }
        return b1;
    }

    /**
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到 小数点以后10位，以后的数字四舍五入。
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(double v1, double v2) {
        return div(v1, v2, DEF_DIV_SCALE);
    }

    /**
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指 定精度，以后的数字四舍五入。
     *
     * @param v1    被除数
     * @param v2    除数
     * @param scale 表示表示需要精确到小数点以后几位。
     * @return 两个参数的商
     */
    public static double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理。
     *
     * @param v     需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理。 超出scale 则处理，没超过直接返回
     *
     * @param v           需要四舍五入的数字
     * @param scale       小数点后保留几位
     * @param isOverScale 是否处理没有到达scale的数组
     * @return 四舍五入后的结果
     */
    public static double round(double v, int scale, boolean isOverScale) {
        if (isOverScale) {
            String s = v + "";
            int position = s.length() - (s.indexOf(".") + 1);
            if (position > 5) {
                if (scale < 0) {
                    throw new IllegalArgumentException("The scale must be a positive integer or zero");
                }
                BigDecimal b = new BigDecimal(Double.toString(v));
                BigDecimal one = new BigDecimal("1");
                return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
            } else {
                return v;
            }
        }
        return v;
    }

    /**
     * 提供精确的小数位四舍五入处理。
     *
     * @param v     需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(BigDecimal v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal one = new BigDecimal("1");
        return v.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位(2位)四舍五入处理。
     *
     * @param v 需要四舍五入的数字
     * @return 四舍五入后的结果
     */
    public static double round(double v) {
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理。
     *
     * @param v     需要四舍五入的数字字符串
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(String v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(v);
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位(2位)四舍五入处理。
     *
     * @param v 需要四舍五入的数字字符串
     * @return 四舍五入后的结果
     */
    public static double round(String v) {
        if (StringUtil.isBlank(v)) {
            return 0;
        }
        BigDecimal b = new BigDecimal(v);
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位处理，去掉保留位数后的数字
     *
     * @param v     需要处理的数字
     * @param scale 小数点后保留几位
     * @return 去掉保留位数后的结果
     */
    public static double decimal(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_DOWN).doubleValue();
    }



        public static String code() {
            String randomcode = "";
            // 用字符数组的方式随机
            String model = "0123456789abcdefghijklmnopqistuvwsyzABCDEFGHIJKLMNOPQISTUVWSYZ";
            char[] m = model.toCharArray();
            for (int j = 0; j < 6; j++) {
                char c = m[(int) (Math.random() * 36)];
                // 保证六位随机数之间没有重复的
                if (randomcode.contains(String.valueOf(c))) {
                    j--;
                    continue;
                }
                randomcode = randomcode + c;
            }
          return randomcode;
        }

}
