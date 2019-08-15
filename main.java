import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.*;
import java.util.*;
import java.nio.file.*;
import java.util.Scanner;

class URI_connect {
    String board_name;
    int number,downloaded_count,vote=0;
    String previous_page_URI;
    public URI_connect(String board_name, int number,int vote) throws IOException { //set the board name and 
        this.board_name = board_name;  //e.g. "Beauty"
        this.number = number;		   //e.g. Recent "100" articles
        this.vote = vote;              //e.g.  score higher than "vote"
        this.previous_page_URI="https://www.ptt.cc/bbs/Beauty/";   //when this page doesn't meet the "number" requirment, go to previous page(older posts) and continue downloading
        while(this.downloaded_count<number){    //check if downloaded posts reach given limit
		System.out.println("現在開始下載"+previous_page_URI+"頁面的文章");
        get_article_list_and_parse(previous_page_URI);  //get_article_list_and_parse will fetch all article in the URI and invoke article_image_download()
		}
		//System.out.println("共成功下載"+this.downloaded_count+"篇文章");
    }
    public URI_connect() {}

    public void get_article_list_and_parse(String targer_URI) throws IOException { //get_article_list_and_parse will fetch all article in the URI and invoke article_image_download()
      
        int article_number = 0;
        String content_temp;
        //Loading first page
        content_temp = URI_get_content(targer_URI);
        //System.out.println(content_temp);

        //get PREVIOUS page html
        Pattern previous_page = Pattern.compile("href=\"(.*)\">&lsaquo; 上頁</a>");
        Matcher previous_page_matcher = previous_page.matcher(content_temp);
        if (previous_page_matcher.find()) {
            this.previous_page_URI = "https://www.ptt.cc" + previous_page_matcher.group(1);   //update the previous page(older )
        }
        //System.out.print(previous_page_URI);
        //get Title vote link List
        Pattern Article_pattern = Pattern.compile("(?s)<div class=\"nrec\">(?:<span class=\".*?\">|)(.*?)(?:</span>|)</div>.*?<div class=\"title\">.*?(?:<a href=\"(.*?)\">(.*?)</a>| ).*?</div>.*?<div class=\"date\"> (.*?)</div>");
        Matcher Article_pattern_matcher = Article_pattern.matcher(content_temp);
        LinkedList<String[]> list=new LinkedList<String[]>();
        while (Article_pattern_matcher.find()) {
			list.addFirst(new String[]{Article_pattern_matcher.group(1).replace("爆","100").replace("X","0"),Article_pattern_matcher.group(2),Article_pattern_matcher.group(3),Article_pattern_matcher.group(4).replace("/","-")});
			//System.out.println(Arrays.toString(info_temp));
            //System.out.println("推文數:"+Article_pattern_matcher.group(1)); //vote
            //System.out.println(Article_pattern_matcher.group(2)); //article_link
            //System.out.println("標題"+Article_pattern_matcher.group(3)); //title
		}
		for(String[] v: list){
			System.out.println(Arrays.toString(v)); //echo the article info
			if(v[1]!=null){
				if(v[2].contains("[公告]")){System.out.println("此篇為公告文");}else{
					if(Integer.parseInt("0"+v[0])>=this.vote || this.vote==0){
						article_image_download("https://www.ptt.cc/"+v[1],v[2],v[3]);
					}else{
						System.out.println("  推文數低於標準"+this.vote+"，不下載此文章");
					}
					if(this.downloaded_count==this.number){   //if the download limit reached, aborted
						break;
					}
				}
			}else{
					System.out.println("  此文章不存在");
			}
            System.out.println(" ");
		}

    }

    public String URI_get_content(String URI) throws IOException { //equal to PHP file_get_contents(URI)
        URL url = new URL(URI);
        URLConnection conn = url.openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        conn.connect();
        String type = conn.getContentType();
        //System.out.println("Context: " + type);

        InputStreamReader in = new InputStreamReader(conn.getInputStream(), "UTF-8");  //get the URI content in UTF-8 encoding
        int data = in .read();
        String content;
        content = "";
        while (data != -1) {
            //System.out.print((char) data);
            content += (char) data;
            data = in .read();
            //fw.write(data);
            //fw.flush();
        }
        return content;
    }

    public void article_image_download(String URI, String title,String date) throws IOException {   //（文章內文的URI, 文章標題, 文章發文日期） 後兩個是給資料夾命名用的
        String content;
        int i;
        content = URI_get_content(URI);
		String directory=date.trim()+title.trim();
		directory=directory.replace(":","").replace("?","");
        Files.createDirectories(Paths.get("./"+directory));
        Pattern pattern = Pattern.compile("nofollow\">.*?imgur.com/(.*?)(?:.jpg|)</a>");   //把imgur開頭的網址都抓出來
        Matcher pattern_matcher = pattern.matcher(content);
        i=0;
        while (pattern_matcher.find()) {
			//System.out.println(pattern_matcher.group(1));
			System.out.println("  嘗試下載http://i.imgur.com/"+pattern_matcher.group(1)+".jpg");  //matcher.group(1)是imgur的圖片代碼
			try(InputStream in = new URL("http://i.imgur.com/"+pattern_matcher.group(1)+".jpg").openStream()){        
				Files.copy(in, Paths.get("./"+directory+"/"+pattern_matcher.group(1)+".jpg"),StandardCopyOption.REPLACE_EXISTING);
				System.out.println("  下載成功");
				i++;       //計算成功下載了幾張圖片
			}catch(Exception ex){
				System.out.println(ex.getClass().getName());
				System.out.println("  http://i.imgur.com/"+pattern_matcher.group(1)+".jpg 下載失敗");
			}
		}
		if(i>0){
			this.downloaded_count++;
			System.out.println("這是第"+this.downloaded_count+"篇文章");
		}else{
			Files.delete(Paths.get("./"+directory));            //如果文章裡面沒有圖片成功下載，就把資料夾刪掉
			System.out.println("沒有下載到圖片，此文章不列入計算");
		}
		System.out.println("共從此文章下載"+i+"張圖片");
    }



}


public class main {

    public static void main(String[] args) throws IOException {
		int number,vote;
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("請問要下載最新的幾篇文章？");
		number=scanner.nextInt();
		System.out.println("請問要下載多少推文數以上的文章（0-100）？  0為不設限");
		vote=scanner.nextInt();
        URI_connect URI = new URI_connect("Beauty", number,vote); //download newest 50 article from board "Beauty" 
		



        //fw.close();

    }

}
