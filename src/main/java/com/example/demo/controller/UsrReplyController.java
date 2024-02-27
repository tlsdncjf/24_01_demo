package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.service.ArticleService;
import com.example.demo.service.BoardService;
import com.example.demo.service.ReactionPointService;
import com.example.demo.service.ReplyModifyRequest;
import com.example.demo.service.ReplyService;
import com.example.demo.util.Ut;
import com.example.demo.vo.Article;
import com.example.demo.vo.Board;
import com.example.demo.vo.Reply;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UsrReplyController {

	@Autowired
	private Rq rq;

	@Autowired
	private ReplyService replyService;

	@Autowired
	private ReactionPointService reactionPointService;

	@RequestMapping("/usr/reply/doWrite")
	@ResponseBody
	public String doWrite(HttpServletRequest req, String relTypeCode, int relId, String body) {

		Rq rq = (Rq) req.getAttribute("rq");

		if (Ut.isNullOrEmpty(relTypeCode)) {
			return Ut.jsHistoryBack("F-1", "relTypeCode을 입력해주세요");
		}
		if (Ut.isEmpty(relId)) {
			return Ut.jsHistoryBack("F-2", "relId을 입력해주세요");
		}
		if (Ut.isNullOrEmpty(body)) {
			return Ut.jsHistoryBack("F-3", "내용을 입력해주세요");
		}

		ResultData<Integer> writeReplyRd = replyService.writeReply(rq.getLoginedMemberId(), relTypeCode, relId, body);

		int id = (int) writeReplyRd.getData1();

		return Ut.jsReplace(writeReplyRd.getResultCode(), writeReplyRd.getMsg(), "../article/detail?id=" + relId);

	}

	@RequestMapping("/usr/reply/doDelete")
	@ResponseBody
	public String doDelete(HttpServletRequest req, int id, String replaceUri) {
		Rq rq = (Rq) req.getAttribute("rq");

		Reply reply = replyService.getReply(id);

		if (reply == null) {
			return Ut.jsHistoryBack("F-1", Ut.f("%d번 댓글은 존재하지 않습니다", id));
		}

		ResultData loginedMemberCanDeleteRd = replyService.userCanDelete(rq.getLoginedMemberId(), reply);

		if (loginedMemberCanDeleteRd.isSuccess()) {
			replyService.deleteReply(id);
		}

		if (Ut.isNullOrEmpty(replaceUri)) {
			switch (reply.getRelTypeCode()) {
			case "article":
				replaceUri = Ut.f("../article/detail?id=%d", reply.getRelId());
				break;
			}
		}

		return Ut.jsReplace(loginedMemberCanDeleteRd.getResultCode(), loginedMemberCanDeleteRd.getMsg(), replaceUri);
	}

	@RequestMapping("/usr/reply/doModify")
	@ResponseBody
	public String doModify(HttpServletRequest req, ReplyModifyRequest replyModifyreq) {

		Integer id = replyModifyreq.getId();
		String body = replyModifyreq.getBody();

		System.err.println("id : " + id);

		if (id == null) {
			// id가 null인 경우, JSON 형식의 에러 메시지 반환
			return Ut.toJsonString(ResultData.from("F-2", "댓글 Id가 유효하지 않습니다."));
		}

		System.err.println("dddddddddddddddddddddddddd");

		Rq rq = (Rq) req.getAttribute("rq");

		Reply reply = replyService.getReply(id);

		if (reply == null) {
			// 해당 ID의 댓글이 존재하지 않는 경우
			return Ut.toJsonString(ResultData.from("F-1", Ut.f("%d번 댓글은 존재하지 않습니다", id)));
		}

		ResultData loginedMemberCanModifyRd = replyService.userCanModify(rq.getLoginedMemberId(), reply);

		if (loginedMemberCanModifyRd.isSuccess()) {
			replyService.modifyReply(id, body);
			// 성공 응답 반환
			return Ut.toJsonString(
					ResultData.from("S-1", "댓글이 수정되었습니다.", "redirectUrl", "../article/detail?id=" + reply.getRelId()));
		} else {
			// 수정 권한이 없는 경우
			return Ut.toJsonString(loginedMemberCanModifyRd);
		}
	}

}