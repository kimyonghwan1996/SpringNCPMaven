package user.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import user.bean.UserImageDTO;
import user.dao.UserUploadDAO;

@Service
public class UserUploadServiceImpl implements UserUploadService {
	@Autowired
	private UserUploadDAO userUploadDAO;
	@Autowired
	private ObjectStorageService objectStorageService;
	@Autowired
	private HttpSession session;
	
	private String bucketName = "bitcamp-6th-bucket-78";
	
	@Override
	public void upload(List<UserImageDTO> userImageList) {
		userUploadDAO.upload(userImageList);
	}

	@Override
	public List<UserImageDTO> getUploadList() {
		return userUploadDAO.getUploadList();
	}

	@Override
	public UserImageDTO getUploadImage(String seq) {

		return userUploadDAO.getUploadImage(seq);
	}

	@Override
	public void userUpload(UserImageDTO userImageDTO, MultipartFile img) {
		//실제폴더
		String filePath = session.getServletContext().getRealPath("WEB-INF/storage");
		System.out.println("실제폴더 = " + filePath);
		
		//DB에서 seq에 해당하는 imageFileName 꺼내와서 object storage(ncp)이미지 삭제
		//object storage는 이미지를 덮어쓰지 못함
		String imageFileName = userUploadDAO.getImageFileName(userImageDTO.getSeq());
		System.out.println("imageFileName = " + imageFileName);
		
		//object storage 이미지 삭제
		objectStorageService.deleteFile(bucketName, "storage/", imageFileName);
		
		//object storage(ncp) 새 이미지 생성
		imageFileName = objectStorageService.uploadFile(bucketName, "storage/", img);
		
		File file = new File(filePath, imageFileName);
		try {
			img.transferTo(file);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		userImageDTO.setImageFileName(imageFileName);
		userImageDTO.setImageOriginalName(img.getOriginalFilename());
		
		//DB
		userUploadDAO.uploadUpdate(userImageDTO);
	}

	@Override
	public void uploadDelete(String[] check) {
		List<String> list = new ArrayList<>();
		
		//DB에서 seq에 해당하는 imageFileName 꺼내와서 list에 담기 ->for each사용하기 위해
		for(String c : check) {
			String imageFileName = userUploadDAO.getImageFileName(Integer.parseInt(c));
			list.add(imageFileName);
		}
		//object storage 삭제
		objectStorageService.deleteFile(bucketName, "storage/", list);
		//DB 삭제
		userUploadDAO.uploadDelete(list);
	}

}
