package com.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Random;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.Charsets;


public class StringUtil {
	
	//------------------------------------------------------------------
	//                   通过正则表达式验证字符串
	//------------------------------------------------------------------
	private static String RegNumber = "^[0-9]+$";
    private static String RegNumberSign = "^[+-]?[0-9]+$";
    private static String RegDecimal = "^[0-9]+[.]?[0-9]*$";
    private static String RegDecimalSign = "^[+-]?[0-9]+[.]?[0-9]*$"; //等价于^[+-]?\d+[.]?\d*$
    private static String RegEmail = "^[\\w-]+@[\\w-]+\\.(com|net|org|edu|mil|tv|biz|info)$";//w 英文字母或数字的字符串，和 [a-zA-Z0-9] 语法一样 
    private static String RegMobile = "^1(3[4-9]|47|5[012789]|8[7-8])\\d{8}$";//验证移动手机号码134.135.136.137.138.139.150.151.152.157.158.159.187.188 ,147
    private static String RegUnicom = "^1(3[012]|5[56]|8[56])\\d{8}$";//验证联通手机号码130.131.132.155.156.185.186
    private static String RegTelecom = "^1([35]3|8[09])\\d{8}$";//验证电信手机号码  133.153.180.189 
    private static String RegImage = "^(jpg|png)$";
    /**判断是否为整数*/
	public static boolean IsNumber(String str){
		  Pattern pattern = Pattern.compile(RegNumber);
		  return pattern.matcher(str).matches();
	}
	/**判断是否为整数 可带正负号*/
    public static boolean IsNumberSign(String str){
    	 Pattern pattern = Pattern.compile(RegNumberSign);
		 return pattern.matcher(str).matches();
    }
    /**是否是浮点数*/
    public static boolean IsDecimal(String str){
    	Pattern pattern = Pattern.compile(RegDecimal);
		return pattern.matcher(str).matches();
    }
    /**是否是浮点数 可带正负号*/
    public static boolean IsDecimalSign(String str){
    	Pattern pattern = Pattern.compile(RegDecimalSign);
		return pattern.matcher(str).matches();
    }
    /**判断是否满足Email格式*/
    public static boolean IsEmail(String str){
    	if(str==null||str.equals("")) return false;
    	Pattern pattern = Pattern.compile(RegEmail);
		return pattern.matcher(str.toLowerCase()).matches();
    }
    /**验证是否为移动号码*/
    public static boolean IsMobile(String str){
    	Pattern pattern = Pattern.compile(RegMobile);
		return pattern.matcher(str).matches();
    }
    /**验证是否为联通号码*/
    public static boolean IsUnicom(String str){
    	Pattern pattern = Pattern.compile(RegUnicom);
		return pattern.matcher(str).matches();
    }
    /**验证是否为电信号码*/
    public static boolean IsTelecom(String str){
    	Pattern pattern = Pattern.compile(RegTelecom);
		return pattern.matcher(str).matches();
    }
    /**判断是否为手机号：包含移动、联通、电信*/
    /**判断是否为手机号：为11位的数字*/
    public static boolean isCellphone(String str){
    	/*boolean tag=false;
    	tag=IsMobile(str);
    	if(tag) return true;
    	tag=IsUnicom(str);
    	if(tag) return true;
    	tag=IsTelecom(str);
    	return tag;*/
    	if(IsNumber(str) && str.length()==11) 
    		return true;
    	return false;
    }
    /**
	 * 验证是否为图片文件
	 * @param fileExt 文件扩展名，不含“.”
	 * @return
	 */
	public static boolean IsImage(String fileExt){
		Pattern pattern = Pattern.compile(RegImage);
		return pattern.matcher(fileExt.toLowerCase()).matches();
	}

    /** 
     * AES加密 
     *  
     * @param content 需要加密的内容 
     * @param password  加密密码 
     * @return 
     */  
    public static byte[] AESEncrypt(String content, String password){
        try {
            byte[] raw = password.getBytes(Charsets.UTF_8);
            if (raw.length != 16) {
                throw new IllegalArgumentException("Invalid key size. " + password + ", 密钥token长度不是16位");
            }

            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16])); // zero IV
            return cipher.doFinal(content.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    /** 
     * AES解密 
     * @param encryptBytes 待解密的byte[] 
     * @param password 解密密钥 
     * @return 解密后的String 
     */  
    public static byte[] AESDecrypt(byte[] encryptBytes, String password){
        try {
            byte[] raw = password.getBytes(Charsets.UTF_8);
            if (raw.length != 16) {
                throw new IllegalArgumentException("Invalid key size. " + password + ", 密钥token长度不是16位");
            }

            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16])); // zero IV
            return cipher.doFinal(encryptBytes);
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return null;
    }
    
	//-------------------------------------------------------------------
	//---------------------      webService操作    ------------------------------
	//-------------------------------------------------------------------
	/**
	 * 通过Url获取相关内容
	 * @param urlStr	拼接好参数
	 * @return
	 */
	public static String getContentByUrl2(String urlStr) throws Exception {
		String content = "";
		URL url = new URL(urlStr);
		URLConnection con = url.openConnection();
		con.setAllowUserInteraction(false);
		InputStream urlStream = con.getInputStream();
		String str;
		BufferedReader br = new BufferedReader(new InputStreamReader(urlStream,"UTF-8"));
		while ((str = br.readLine()) != null) {
			content += str;
		}
		br.close();
		return content;
	}
	/**
	 * 通过Url获取相关内容
	 * @param urlStr	拼接好参数
	 * @return
	 */
	public static String getContentByUrl(String urlStr) {
		try {
            URL urlGet = new URL(urlStr);
            HttpURLConnection http = (HttpURLConnection) urlGet.openConnection();    
            http.setRequestMethod("GET");      //必须是get方式请求    
            http.setRequestProperty("Content-Type","application/x-www-form-urlencoded");    
            http.setDoOutput(true);        
            http.setDoInput(true);
            System.setProperty("sun.net.client.defaultConnectTimeout", "30000");//连接超时30秒
            System.setProperty("sun.net.client.defaultReadTimeout", "90000"); //读取超时30秒
            http.connect();
            InputStream is =http.getInputStream();
            int size =is.available();
            byte[] jsonBytes =new byte[size];
            is.read(jsonBytes);
            String message=new String(jsonBytes,"UTF-8");
            return message;
    	} catch (Exception e) {
          	e.printStackTrace();
      	}
    	return "";
	}
	public static String getContentGETUrl(String urlStr) throws Exception{
		String content = "";
		URL url = new URL(urlStr);
		URLConnection con = url.openConnection();
		con.setAllowUserInteraction(false);
		InputStream urlStream = con.getInputStream();
		String str;
		BufferedReader br = new BufferedReader(new InputStreamReader(urlStream,"UTF-8"));
		while ((str = br.readLine()) != null) {
			content += str;
		}
		br.close();
		return content;
	}
	/**
	 * 通过Post方法提交数据
	 * @param url		拼接好url
	 * @param params	要上传的数据(可以是Json格式)
	 * @return
	 */
	public static String postData(String urlStr,String params){
		try{        
			URL url = new URL(urlStr);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();    
			http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
	        http.setRequestMethod("POST");
	        http.setRequestProperty("Content-Type","application/x-www-form-urlencoded/json");    
	        
	        http.setDoOutput(true);        
	        http.setDoInput(true);
	        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");//连接超时30秒
	        System.setProperty("sun.net.client.defaultReadTimeout", "30000"); //读取超时30秒
	        http.connect();
	        OutputStream os= http.getOutputStream();    
            os.write(params.getBytes("UTF-8"));//传入参数    
            os.flush();
            os.close();
	        InputStream is =http.getInputStream();
	        int size =is.available();
	        byte[] jsonBytes =new byte[size];
	        is.read(jsonBytes);
	        String message=new String(jsonBytes,"UTF-8");
	        return message;
		}catch(Exception e){
			e.printStackTrace();
		}
		return "异常";
	}
	
	//-------------------------------------------------------------------
	//---------------------      字符串加密操作    ------------------------------
	//-------------------------------------------------------------------
	/**sha1加密*/   
    public static String SHA1Encode(String sourceString) {  
        String resultString = null;  
        try {  
           resultString = new String(sourceString);  
           MessageDigest md = MessageDigest.getInstance("SHA-1"); 
           byte[] bytes = md.digest(resultString.getBytes());
           StringBuffer buf = new StringBuffer(bytes.length * 2);  
           for (int i = 0; i < bytes.length; i++) {  
               if (((int) bytes[i] & 0xff) < 0x10) {  
                   buf.append("0");  
               }  
               buf.append(Long.toString((int) bytes[i] & 0xff, 16));  
           }  
           resultString =  buf.toString().toUpperCase(); 
        } catch (Exception ex) {  
        }  
        return resultString;  
    }
    /**MD5加密*/
	public static String MD5Encode(String s) {
		try {
			byte[] b = s.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(b);
			b = md.digest();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < b.length; i++) {
				if ((b[i] & 0xff) < 0x10) {
					buf.append("0");
				}
				buf.append(Long.toString(b[i] & 0xff, 16));
			}
			return buf.toString();
		} catch (Exception e) {
			return s;
		}
	}
	/**
	 * 指定加密方式对字符串加密
	 * @param password		要加密的字符串
	 * @param algorithm		加密方式  "MD5"
	 * @return
	 */
    public static String encodePassword(String password, String algorithm) {
		byte[] unencodedPassword = password.getBytes();
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(algorithm);
		} catch (Exception e) {
			return password;
		}
		md.reset();
		md.update(unencodedPassword);
		byte[] encodedPassword = md.digest();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < encodedPassword.length; i++) {
			if ((encodedPassword[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
		}
		return buf.toString();
	}
    
	/**得到Unicode字符串*/
	public static String getUnicodeString(String s,String formCode,String latterCode){
		String s1 = null;
		try {
			if(s!=null){
				s1 = new String(s.getBytes(formCode),latterCode);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return s1;
	}
	
	//-------------------------------------------------------------------
	//---------------------      地址栏传参过滤    ------------------------------
	//-------------------------------------------------------------------
	/**
	 * 过滤XSS攻击代码,同时转义HTML代码
	 * @param v	待处理的字串
	 * @return
	 */
	public static String filterXSSCode(String v) {
		if(v==null || v.equals(""))return v;
		//v = Utilities.escapeHTML(v);
		if(v.indexOf("<aifei>")==-1){
			v = escapeHTML(v,true);
		}
		v = v.replace("javascript:","");
		v = v.replace("expression:","");
		v = v.replace("onerror:","");
		v = v.replace("onError:","");
		v = v.replace("onload:","");
		v = v.replace("onLoad:","");
		return v;
	}
	/**
     * Remove occurences of html, defined as any text
     * between the characters "&lt;" and "&gt;".
     * Optionally replace HTML tags with a space.
     *
     * @param str
     * @param addSpace
     * @return
     */
	public static String removeHTML(String str, boolean addSpace) {
        if (str == null) return "";
        StringBuffer ret = new StringBuffer(str.length());
        int start = 0;
        int beginTag = str.indexOf("<");
        int endTag = 0;
        if (beginTag == -1)
            return str;
        
        while (beginTag >= start) {
            if (beginTag > 0) {
                ret.append(str.substring(start, beginTag));
                
                // replace each tag with a space (looks better)
                if (addSpace) ret.append(" ");
            }
            endTag = str.indexOf(">", beginTag);
            
            // if endTag found move "cursor" forward
            if (endTag > -1) {
                start = endTag + 1;
                beginTag = str.indexOf("<", start);
            }
            // if no endTag found, get rest of str and break
            else {
                ret.append(str.substring(beginTag));
                break;
            }
        }
        // append everything after the last endTag
        if (endTag > -1 && endTag + 1 < str.length()) {
            ret.append(str.substring(endTag + 1));
        }
        return ret.toString().trim();
    }
	/**
     * Escape, but do not replace HTML.
     * @param escapeAmpersand Optionally escape
     * ampersands (&amp;).
     */
    public static String escapeHTML(String s, boolean escapeAmpersand) {
        // got to do amp's first so we don't double escape
        if (escapeAmpersand) {
            s = s.replace( "&", "&amp;");
        }
        s = s.replace(  "&nbsp;", " ");
        s = s.replace(  "\"", "&quot;");
        s = s.replace(  "<", "&lt;");
        s = s.replace(  ">", "&gt;");
        return s;
    }
	//------------------------------------------------------------------
	//------------------------------------------------------------------

	/**
	 * 字符串截取
	 * @param s		要截取的字符串
	 * @param num	要保留的字数
	 * @return
	 */
	public String subString(String s,int num){
		if(s==null||s.equals(""))return s;
		String regEx="[\\u4e00-\\u9fa5]";
		Pattern p = Pattern.compile(regEx);
		String ret="";
		int n=0;
		for(int i=0;i<s.length();i++){
			char c=s.charAt(i);
			n+=1;
			if(p.matcher(c+"").find())n+=1;
			ret+=c;
			if(n>=num)break;
		}
		if(s.equals(ret)) return s;
		ret+="...";
		return ret;
	}
	/**
	 * 根据当前时间指定格式生成编码 + 随机数
	 * @param pattern	时间格式：yyyyMMddHHmmss
	 * @param k	指定随机数位数
	 * @return
	 */
	public static String getNumber(String pattern,int k){
		String number=DateTimeUtil.nowString(pattern);
		number+=getRandom(k);
		return number;
	}
	/**
	 * 获取指定位数的随机数
	 * @param k	指定随机数位数
	 * @return
	 */
	public static String getRandom(int k){
		if(k==0)	return "";
		Random r = new Random();
		String str="";
		for(int i=0;i<k;i++){
			str+=r.nextInt(9);
		}
		return str;
	}
	/**数组转字符串*/   
    public static String arrayToString(String [] arr){  
        StringBuffer bf = new StringBuffer();  
        for(int i = 0; i < arr.length; i++){  
         bf.append(arr[i]);  
        }  
        return bf.toString();  
    }  
    
}
