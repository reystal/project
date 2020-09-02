package api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.Image;
import dao.ImageDao;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImageServlet extends HttpServlet
{
    /**
     * 查看图片属性：既能查看所有图片，也能查看指定图片
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // 考虑到查看所有图片属性和查看指定图片属性
        // 通过是否URL中带有imageId参数来进行区分
        // 存在imageId查看指定图片属性，否则就查看所有图片属性
        String imageId=req.getParameter("imageId");
        if(imageId==null||imageId.equals(""))
        {
            //查看所有图片属性
            selectAll(req,resp);
        }
        else
        {
            //查看指定图片属性
            selectOne(imageId,resp);
        }
    }
    private void selectAll(HttpServletRequest req,HttpServletResponse resp) throws IOException
    {
        resp.setContentType("application/json;charset=utf-8");
        // 1.创建一个ImageDao 对象，并查找数据库
        ImageDao imageDao= new ImageDao();
        List<Image> images=imageDao.selectAll();
        // 2.把查找到的结果转成JSON格式的字符串，并且回给resp对象
        Gson  gson=new GsonBuilder().create();
        // jsonData就是一个JSON格式的字符串，和之前约定好的格式一致

        //gson自动帮我们完成了大量的格式转换工作，只要之前的相关字段都约定成统一命名，则此操作就可一步到位完成转换
        String jsonData=gson.toJson(images);
        resp.getWriter().write(jsonData);

    }
    private void selectOne(String imageId,HttpServletResponse resp) throws IOException
    {
        resp.setContentType("application/json;charset=utf-8");
        // 1. 创建ImageDao对象
        ImageDao imageDao=new ImageDao();
        Image image=imageDao.selectOne(Integer.parseInt(imageId));
        // 2.使用gson 把查到的数据转成json格式，并写回给响应对象
        Gson gson=new GsonBuilder().create();
        String jsonData=gson.toJson(image);
        resp.getWriter().write(jsonData);
    }

    /**
     * 上传图片
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // 1.获取图片的属性信息，并且存入数据库
        //a)需要创建一个factory对象和upload对象,这是为了获取图片的属性做的准备工作
        // 固定逻辑
        FileItemFactory factory=new DiskFileItemFactory();
        ServletFileUpload upload=new ServletFileUpload(factory);
        //b）通过upload对象进一步解析请求（解析HTTP请求中奇怪的body中的内容）
        //   FileItem就代表一个上传的文件对象
        //理论上来说，HTTP支持一个请求中间同时上传多个文件
        List<FileItem> items=null;
        try {
           items = upload.parseRequest(req);
        } catch (FileUploadException e)
        {
            // 出现异常说明解析出错
            e.printStackTrace();

            //告诉客户端出现的具体的错误是啥
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write("{\"ok\":false,\"reason\":\"请求解析失败\"}");
            return;
        }
         // c）把FileItem中的属性提取出来，转换成Image对象，才能存到数据库中
        //  当前只靠虑一张图片的情况
        FileItem fileItem=items.get(0);
        Image image =new Image();
        image.setImageName(fileItem.getName());
        image.setSize((int)fileItem.getSize());
        // 手动获取当前日期，并转成格式化日期
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd");
        image.setUploadTime(simpleDateFormat.format(new Date()));
        image.setContentType(fileItem.getContentType());
        // 自己构造一个路径来保存,引入时间戳是为了让文件路径能够唯一
        image.setPath("./image/"+System.currentTimeMillis()+"_"+image.getImageName());
        // MD5暂时不计算
        image.setMd5("11223344");
        //存到数据库中
        ImageDao imageDao=new ImageDao();
        imageDao.insert(image);

        // 2.获取图片的内容信息，并且写入磁盘文件
        File file =new File(image.getPath());
        try {
            fileItem.write(file);
        } catch (Exception e)
        {
            e.printStackTrace();
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write("{\"ok\":false,\"reason\":\"写入磁盘失败\"}");
            return;
        }

        // 3.给客户端返回一个结果数据
       // resp.setContentType("application/json;charset=utf-8");
        //resp.getWriter().write("{\"ok\":true}");
        resp.sendRedirect("index.html");
    }

    /**
     * 删除指定图片
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setContentType("application/json;charset=utf-8");
        // 1.先获取到请求中的imageId
        String imageId =req.getParameter("imageId");
        if(imageId==null||imageId.equals(""))
        {
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":false, \"reson\":\"请求解析失败\"}");
            return;
        }
        // 2.创建ImageDao对象，查看到该图片对象对应的相关属性（这是为了知道这个图片对应的文件路径）
        ImageDao imageDao =new ImageDao();
        Image image=imageDao.selectOne(Integer.parseInt(imageId));
        if(image==null)
        {
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":false, \"reson\":\"imageId在数据库中不存在\"}");
            return;

        }
        // 3.删除数据库中的记录
        imageDao.delete(Integer.parseInt(imageId));
        // 4. 删除本地磁盘文件
        File file = new File(image.getPath());
        file.delete();
        resp.setStatus(200);
        resp.getWriter().write("{\"ok\":true}");
    }
}
