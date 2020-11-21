package com.kh.john.exboard.controller;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.john.common.exception.RequestDuplicateException;
import com.kh.john.exboard.model.service.ExboardService;
import com.kh.john.exboard.model.vo.ExpertBoard;
import com.kh.john.exboard.model.vo.SessionVo;
import com.kh.john.member.controller.AES256Util;
import com.kh.john.member.controller.MailHandler;
import com.kh.john.member.model.vo.Expert;
import com.kh.john.member.model.vo.Member;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ExboardController {

	@Autowired
	private ExboardService service;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private AES256Util aes;

	// 전문가 페이지 임시 메인
	@RequestMapping("/expert")
	public ModelAndView expertPage(HttpSession session) {
		log.debug("expertPage 실행");
		ModelAndView mv = new ModelAndView("/exboard/exboardMain");
		return mv;
	}

	// 전문가 리스트들 불러오기
	@RequestMapping("/expert/expertPrintList")
	public ModelAndView expertPrintList(@RequestParam(required = false) String ca) {
		log.debug("expertPrintList 실행");
		log.debug("ca : " + ca);
		ModelAndView mv = new ModelAndView("/exboard/expertList");
		try {
			List<Map<String, Object>> list = service.selectExpert(ca);
			for (Map<String, Object> mm : list) {
				log.debug(mm.get("mem").toString());
			}
			mv.addObject("list", list);
			mv.addObject("exbolist", service.selectExboardListCategory(ca));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("전문가 리스트 불러오기 실패");
		}
		return mv;
	}

	// 전문가가 자신한테 유저들이 신청한 리스트 출력하기
	@RequestMapping("/expert/expertRequestPrintList")
	public ModelAndView expertRequestPrintList(HttpSession session) {
		log.debug("expertRequestPrintList 실행");
		ModelAndView mv = new ModelAndView("/exboard/expertRequestList");
		Member mem = (Member) session.getAttribute("loginMember");
		int rlistCount = 0;
		try {

			rlistCount = service.selectExpertRequestAjaxCount(mem);
//			List<ExpertRequest> rlist = service.selectExpertRequest(mem);
//			List<ExpertBoard> blist = service.selectExpertBoard(mem);
//
//			for (ExpertRequest er : rlist) {
//				log.debug("확인용5");
//				if (blist.size() == 0) {
//					er.setStartCounsel(false);
//					log.debug("확인용4");
//				} else {
//					for (ExpertBoard eb : blist) {
//						if (er.getExpertRequestMemUsid() == eb.getExpertBoardMemUsid()) {
//							// 이미 상담 게시판이 만들어진 유저
//							log.debug("확인용1");
//							er.setStartCounsel(true);
//							if (eb.getExpertBoardExpertend() == 1) {
//								log.debug("확인용2");
//								er.setEndCounsel(true);
//							} else {
//								er.setEndCounsel(false);
//							}
//							break;
//						} else {
//							log.debug("확인용3");
//							er.setStartCounsel(false);
//						}
//					}
//				}
//			}
//
//			mv.addObject("list", rlist);

			mv.addObject("totalData", rlistCount);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("상담 유저 리스트 불러오기 실패");
		}
		return mv;
	}

//상담 희망시간, 요구사항 받아서 넘기기
	// 상담 종료후에도 평점이랑 후기 적기
//https://www.cssscript.com/accessible-star-rating-system-pure-css/

	// 전문가 상세 프로필 보는곳 여기서 상담 신청 가능
	@RequestMapping("/expert/expertApply")
	public ModelAndView expertApply(String no, String nic, HttpSession session) {
		log.debug("expertApply 실행");
		log.debug("no : " + no + " nic : " + nic);
		Member mem = (Member) session.getAttribute("loginMember");
		Member expert = new Member();
		expert.setUsid(Integer.parseInt(no));
		expert.setMemNickname(nic);
		ModelAndView mv = new ModelAndView("/exboard/expertApply");
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("exusid", "" + expert.getUsid());
			map.put("memusid", "" + mem.getUsid());
			Member mm = service.selectMember(no);
			try {
				mm.setMemEmail(aes.decrypt(mm.getMemEmail()));
				mm.setTel(aes.decrypt(mm.getTel()));
			} catch (Exception e) {

			}
			mv.addObject("mem", mm);
			mv.addObject("expert", service.selectExpertMem(no));
			mv.addObject("license", service.selectExpertLicense(no));
			mv.addObject("requestIsDuplicate", service.selectIsDuplicateReq(map));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("전문가 정보 불러오기 실패");
		}
		return mv;
	}

	// 상담 신청 버튼 눌렀을때
	@ResponseBody
	@RequestMapping("/expert/expertRequest")
	public String expertRequest(String no, String nic, String time, String applyText, HttpSession session) {
		log.debug("expertRequest 실행");
		log.debug("no : " + no + " nic : " + nic + " time : " + time + " applyText : " + applyText);
		Member mem = (Member) session.getAttribute("loginMember");
		Member expert = new Member();
		expert.setUsid(Integer.parseInt(no));
		expert.setMemNickname(nic);
		time = time.replace("T", " ");

		if (time.equals("") || time == null) {
			log.debug("시간을 선택해주세요");
			return "9998";
		}

		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date time_ = null;
		try {
			time_ = format1.parse(time);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Date now = new Date();
		int compare = now.compareTo(time_);
		if (compare > 0) {
			log.debug("현재 시간보다 과거를 선택했음");
			return "9999";
		}

		Map<String, String> map = new HashMap<String, String>();
		map.put("exusid", "" + expert.getUsid());
		map.put("exnick", expert.getMemNickname());
		map.put("memusid", "" + mem.getUsid());
		map.put("memnick", mem.getMemNickname());
		map.put("time", time);
		map.put("applyText", applyText);

		String result = "";
		try {
			service.insertExpertMemRequest(map);
			result = "1";
		} catch (RequestDuplicateException e) {
			result = "2";
		} catch (Exception e) {
			result = "0";
		}
		return result;

	}

	// 상담 신청 취소 버튼 눌렀을때
	@ResponseBody
	@RequestMapping("/expert/expertRequestCancel")
	public String expertRequestCancel(String no, String nic, HttpSession session) {
		log.debug("expertRequestCancel 실행");
		log.debug("no : " + no + " nic : " + nic);
		Member mem = (Member) session.getAttribute("loginMember");
		Member expert = new Member();
		expert.setUsid(Integer.parseInt(no));
		expert.setMemNickname(nic);

		String result = "";
		try {
			service.deleteExpertMemRequest(expert, mem);
			result = "1";
		} catch (Exception e) {
			result = "0";
		}
		return result;
	}

	// 전문가 게시판을 생성하고 해당 게시판의 ID값을 가져와서 반환함
	@ResponseBody
	@RequestMapping("/expert/selectExpertBno")
	public String selectExpertBno(HttpSession session, String no, String nic) {
		String result = "";
		Member expertmem = (Member) session.getAttribute("loginMember");
		try {
			result = "" + service.insertExpertBoard(no, expertmem);
			log.debug("result : " + result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// 상담 게시판 개설
	@RequestMapping("/expert/counselStart")
	public String counselStart(HttpServletRequest req, String no, String nic, String bno,
			RedirectAttributes redirectAttributes) {
		log.debug("counselStart 실행");
		log.debug("no : " + no + " nic : " + nic);
		String context = "";
		log.debug("bno : " + bno);
		int result = Integer.parseInt(bno);
		try {
			Member m = service.selectMember(no);
			String email = m.getMemEmail();
			email = aes.decrypt(email);
			log.debug("email : " + email);
			int n = (req.getRequestURL()).indexOf(req.getRequestURI());
			context = (req.getRequestURL()).substring(0, n);
			MailHandler sendMail = new MailHandler(mailSender);
			sendMail.setSubject("상담 채팅 요청이 도착했습니다.)");
			String html = context + "/john/expert/expertRoom?bno=" + result;
			log.debug("html : " + html);
			sendMail.setText(new StringBuffer().append(html + "<br> 링크를 클릭해서 상담으로 바로가기").toString());
			sendMail.setFrom("minsu87750@gmail.com", "재판하는 존경장님");
			sendMail.setTo(email);
			sendMail.send();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("result : " + result);
		redirectAttributes.addAttribute("bno", result);
		/*
		 * redirectAttributes.addAttribute("no", no);
		 * redirectAttributes.addAttribute("nic", nic);
		 */
		return "redirect:/expert/expertRoom";
	}

	// 에러 메세지용 메소드
	public ModelAndView gotoMsg(ModelAndView mv, String loc, String msg) {
		mv = new ModelAndView("/common/msg");
		mv.addObject("loc", loc);
		mv.addObject("msg", msg);
		return mv;
	}

	// 채팅창 입장
	@RequestMapping("/expert/expertRoom")
	public ModelAndView expertRoom(@RequestParam("bno") String bnum, HttpSession session) {
		log.debug("expertRoom 실행");
		ModelAndView mv = new ModelAndView("/exboard/exchatRoom");
		Member m = (Member) session.getAttribute("loginMember");
		SessionVo s = new SessionVo();
		ExpertBoard eb = null;
		log.debug("m : " + m);
		// 해당 게시판 넘버에 맞는 유저를 판별하기 위해서 가져옴
		try {
			eb = service.selectExpertBoard(bnum);
			log.debug("eb : " + eb);

			// 쿼리스트링에 방넘버가 없거나 있지도 않는 방에 접근할때
			if (eb == null || bnum.equals("") || bnum == null) {
				log.debug("잘못된 접근");
				mv = gotoMsg(mv, "/", "잘못된 접근입니다.");
				return mv;
			}

			// log.debug(" 상담 결과 : " + eb.getExpertBoardAdviceResult());
			if (m.getMemClass().equals("전문가")) {
				// log.debug("전문가");
				if (m.getUsid() != eb.getExpertBoardUsid()) {
					// log.debug("잘못된 접근");
					mv = gotoMsg(mv, "/", "잘못된 접근입니다.");
					return mv;
				} else if (eb.getExpertBoardAdviceResult() != null) {
					// log.debug("이미 만료된 상담입니다.");
					mv = gotoMsg(mv, "/", "만료된 상담입니다.");
					return mv;
				}
				s.setExpert(true);
			} else {
				// log.debug("전문가 아님");
				if (m.getUsid() != eb.getExpertBoardMemUsid()) {
					// log.debug("잘못된 접근2");
					mv = gotoMsg(mv, "/", "잘못된 접근입니다.");
					return mv;
				} else if (eb.getExpertBoardAdviceResult() != null) {
					// log.debug("이미 만료된 상담입니다.");
					mv = gotoMsg(mv, "/", "만료된 상담입니다.");
					return mv;
				}
				s.setExpert(false);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		s.setCurRoomBid(bnum);
		s.setNickname(m.getMemNickname());
		s.setSessionUsid(m.getUsid());
		session.setAttribute("loginnedMember", s);
		mv.addObject("bno", bnum);
		mv.addObject("eb", eb);
		return mv;
	}

	// 상담 게시판 접속
	@RequestMapping("/expert/counselConn")
	public String counselConnction(HttpSession session, String no, String nick, RedirectAttributes redirectAttributes) {
		log.debug("counselConnction 실행");
		Member expertmem = (Member) session.getAttribute("loginMember");
		log.debug("no : " + no + " nick : " + nick);
		int bno = 0;
		try {
			bno = service.selectExBoardNum(expertmem, no);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		redirectAttributes.addAttribute("bno", bno);
		/*
		 * redirectAttributes.addAttribute("no", no);
		 * redirectAttributes.addAttribute("nic", nick);
		 */
		return "redirect:/expert/expertRoom";
	}

	// 에러 메세지 매핑용
	@RequestMapping(value = "/msg")
	public ModelAndView error() throws Exception {
		log.info(" ===== error 실행 ===== ");
		ModelAndView mv = new ModelAndView("/common/msg");
		return mv;
	}

	// 상담 종료
	@RequestMapping(value = "/expert/counselEnd")
	public String counselEnd(String extext, String bno, RedirectAttributes redirectAttributes) throws Exception {
		log.info(" ===== counselEnd 실행 ===== ");

		log.debug("extext : " + extext);

		int result = service.updateCounselResult(extext, bno);

		if (result == 1) {// 1이면 정상 종료
			return "redirect:/expert/expertRequestPrintList";
		}
		// 아니면 비정상 종료
		redirectAttributes.addAttribute("loc", "/expert/expertRequestPrintList");
		redirectAttributes.addAttribute("msg", "잘못된 종료 관리자에게 문의하세요");
		return "redirect:/msg";

	}

	// 파일 업로드용 전문가 상담
	@ResponseBody
	@RequestMapping("/expert/upload")
	public String expertuUploadll(MultipartFile[] upFile, HttpServletRequest request) {
		log.debug("expertuUploadll 실행");
		String result = "";
		String path = request.getServletContext().getRealPath("/resources/upload_images");
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < upFile.length; i++) {
			log.debug(" ================================ ");
			log.debug("파일명 : " + upFile[i].getOriginalFilename());
			log.debug("파일크기 : " + upFile[i].getSize());
		}
		File dir = new File(path);
		if (!dir.exists()) {
			// 지정된 경로의 폴더가 없으면
			dir.mkdirs();// s넣으면 중간 경로 없어도 알아서 만들어주는것!!
		}

		// 파일 업로드 로직 처리하기
		for (MultipartFile f : upFile) {
			if (!f.isEmpty()) {
				// 전달된 파일이 있으면... 파일 업로드 처리
				// 파일 리네임 처리 -> 중복방지를 위해!
				String originalFileName = f.getOriginalFilename();
				String ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmssSSS");
				int rndNum = (int) (Math.random() * 1000);
				String renamedFileName = sdf.format(new Date(System.currentTimeMillis())) + "_" + rndNum + "." + ext;
				try {
					// renamedFileName 으로 파일을 저장하기 -> transferTo(파일)
					f.transferTo(new File(path + "/" + renamedFileName));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				list.add(renamedFileName);
			}
		}
		try {
			result = new ObjectMapper().writeValueAsString(list);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// 회원 정보보기
	@RequestMapping(value = "/expert/memInfo")
	public ModelAndView memInfo(@RequestParam(required = false) String usid, @RequestParam(required = false) String bno,
			@RequestParam(required = false) String musid) throws Exception {
		log.info(" ===== exmemInfo 실행 ===== ");
		log.debug("usid : " + usid + ", bno : " + bno);
		ModelAndView mv = new ModelAndView("/exboard/exboardMemInfo");

		Member enterMem = service.selectMember(usid);
		String searchMemUsid = null;
		Member searchMem = null;
		Expert ex = null;

		// 보드 넘버가 파라미터로 안넘어왔으면 두개의 USID로 가져옴
		if (bno == null || bno.equals("")) {
			Map<String, String> bomap = new HashMap<String, String>();
			bomap.put("musid", musid);
			bomap.put("usid", usid);
			log.debug("bno null");
			bno = service.selectExBoardNumUsid(bomap);
		}

		// 현재 실행한 유저가 전문가인지 아닌지를 분기로 전문가면 같은 방의 유저를 반환, 유저면 전문가 정보를 반환
		Map<String, String> map = new HashMap<String, String>();
		if (enterMem.getMemClass().equals("전문가")) {
			// 일반 유저 아이디
			// 이 부분 이렇게 안해도될꺼같은데 나중에 보고 지우기
//			if (bno != null) {
//				map.put("expertusid", "" + enterMem.getUsid());
//				map.put("bno", bno);
//				log.debug("expertusid : " + map.get("expertusid") + " bno : " + map.get("bno"));
//				searchMemUsid = service.selectMemExboard(map);
//				log.debug("searchMemUsid : " + searchMemUsid);
//				searchMem = service.selectMember(searchMemUsid);
//				log.debug("searchMem : " + searchMem);
//				searchMem.setMemEmail(aes.decrypt(searchMem.getMemEmail()));
//				mv.addObject("m", searchMem);
//			} else if (bno == null) {
			Member noboard = service.selectMember(musid);
			noboard.setMemEmail(aes.decrypt(noboard.getMemEmail()));
			mv.addObject("m", noboard);
			// }

		} else if (enterMem.getMemClass().equals("일반유저")) {
			map.put("memusid", "" + enterMem.getUsid());
			map.put("bno", bno);
			searchMemUsid = service.selectExpertExboard(map);
			searchMem = service.selectMember(searchMemUsid);
			ex = service.selectExpertMem("" + searchMem.getUsid());
			searchMem.setMemEmail(aes.decrypt(searchMem.getMemEmail()));
			mv.addObject("m", searchMem);
			mv.addObject("ex", ex);
		}
		return mv;
	}

	// 유저가 고민 해결을 누르면 실행됨 그 다음에 상담사가 상담 종료 가능
	@ResponseBody
	@RequestMapping("/expert/counselMemberEnd")
	public String counselMemberEnd(String bno) {
		String result = "";
		// expertBoardMemberend의 값을 1로 바꾸기
		try {
			service.updateCounselMemberEnd(bno);
			result = "1";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = "0";
		}
		return result;
	}

//	Member mem = (Member) session.getAttribute("loginMember");
//	try {
//
//		List<ExpertRequest> rlist = service.selectExpertRequest(mem);
//		List<ExpertBoard> blist = service.selectExpertBoard(mem);
//
//		for (ExpertRequest er : rlist) {
//
//			if (blist.size() == 0) {
//				er.setStartCounsel(false);
//			} else {
//				for (ExpertBoard eb : blist) {
//					if (er.getExpertRequestMemUsid() == eb.getExpertBoardMemUsid()) {
//						// 이미 상담 게시판이 만들어진 유저
//						er.setStartCounsel(true);
//						if (eb.getExpertBoardAdviceResult() != null) {
//							er.setEndCounsel(true);
//						}
//						break;
//					} else {
//						er.setStartCounsel(false);
//					}
//				}
//			}
//		}

	/// expert/selectExpertListAjax
//	@ResponseBody
//	@RequestMapping(value = "/expert/selectExpertListAjax", produces = "application/json; charset=utf8")
//	public String selectExpertListAjax(HttpServletResponse response, String sort, String page, String searchType,
//			String searchInput, String cpage, HttpSession session) {
//
//		log.debug("sort : " + sort + " page : " + page + " st : " + searchType + " si : " + searchInput + " cpage : "
//				+ cpage);
//		String result = "";
//		Member mem = (Member) session.getAttribute("loginMember");
//		try {
//			Map<String, String> map = new HashMap<String, String>();
//			map.put("mem", "" + mem.getUsid());
//			map.put("sort", sort);
//			map.put("page", page);
//			map.put("searchType", searchType);
//			map.put("searchInput", searchInput);
//			map.put("cpage", cpage);
//			List<ExpertRequest> list = service.selectExpertRequestAjax(map);
//			List<ExpertBoard> blist = service.selectExpertBoard(mem);
//
//			for (ExpertRequest er : list) {
//
//				SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
//				String extime_ = format1.format(er.getExpertDate());
//				er.setExpertDateTmp(extime_);
//
//				if (blist.size() == 0) {
//					er.setStartCounsel(false);// 신청 내역이 없는 상태 전부 false로 만들어줌
//					er.setEndCounsel(false);
//				} else {
//					for (ExpertBoard eb : blist) {
//						if (er.getExpertRequestMemUsid() == eb.getExpertBoardMemUsid()) {
//							// expert_board가 있는 상태
//							er.setStartCounsel(true);
//
//							if (er.getExpertIscounsel() == 1 && eb.getExpertBoardExpertend() == 1) {
//								// 상담 끝난 상태
//								er.setEndCounsel(true);
//							} else {
//								er.setEndCounsel(false);
//							}
//
//						} else {
//							// 상담 시작 안했다는것 expert_board가 없는 상태
//							er.setEndCounsel(false);
//						}
//
//					}
//
//				}
//
//			}

//			for (ExpertRequest er : list) {
//
//				SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
//				String extime_ = format1.format(er.getExpertDate());
//				er.setExpertDateTmp(extime_);
//
//				log.debug("확인용5");
//				if (blist.size() == 0) {
//					er.setStartCounsel(false);
//					log.debug("확인용4");
//				} else {
//					for (ExpertBoard eb : blist) {
//						if (eb.getExpertBoardUsid() == mem.getUsid()) {
//							if (er.getExpertRequestMemUsid() == eb.getExpertBoardMemUsid()) {
//								// 이미 상담 게시판이 만들어진 유저
//								log.debug("확인용1");
//								er.setStartCounsel(true);
//								if (eb.getExpertBoardExpertend() == 1) {
//									log.debug("확인용2");
//									er.setEndCounsel(true);
//								} else {
//									er.setEndCounsel(false);
//								}
//								break;
//							} else {
//								log.debug("확인용3");
//								er.setStartCounsel(false);
//							}
//						} else {
//							er.setStartCounsel(false);
//						}
//					}
//				}
//			}
//
//	for(
//
//	ExpertRequest er:list)
//	{
//		log.debug("er : " + er.getEndCounsel() + " name : " + er.getExpertRequestMemNick() + " start : "
//				+ er.getStartCounsel());
//	}
//
//	result=new ObjectMapper().writeValueAsString(list);}catch(
//	Exception e)
//	{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}return result;
//}

}
