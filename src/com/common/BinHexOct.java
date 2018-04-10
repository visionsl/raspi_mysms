package com.common;

public class BinHexOct {
	
	/**
	 * 输出结果, 把5位的字节数组从前到后组成5位的字节
	 * @param res	{0x11,0x22,0x33,0x44,0x55}  ( 转成0x1122334455L )
	 * @return		返回对应len长度Long类型
	 */
	public static long splitHEX(long[] res){
		int offset=(res.length-1)*8;
		long u=0;
		for(int i=0;i<res.length;i++){
			long u2=res[i]<<(offset-(i*8));
			u+=u2;
		}
		return u;
	}
	
	/**
	 * 输出结果, 把5位字节按从高到低拆成5个数组
	 * @param u64	0xABCDABCD71L
	 * @param len	字节长度, 例如 "0xABCDABCD71L" = 5字节长
	 * @return		返回对应len长度short类型数组
	 */
	public static short[] splitHEX_array(long u64, int len){
		short dds[] = new short[len];		//输出结果, 把5位字节按从高到低拆成5个数组
		long u1 = 0;
		int fi = len;				//移位计算下标
		for(int i=0;i<len;i++){
			int offset = fi*8;
			u1 = u1 << offset;
			u64 = u64-u1;
			fi--;offset = fi*8;
			u1 = u64 >> offset;
			dds[i] = (short)u1;
		}
		return dds;
	}
}
