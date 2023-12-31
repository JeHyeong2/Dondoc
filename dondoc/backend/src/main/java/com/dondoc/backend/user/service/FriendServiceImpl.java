package com.dondoc.backend.user.service;

import com.dondoc.backend.common.exception.NotFoundException;
import com.dondoc.backend.user.dto.friend.FriendListDto;
import com.dondoc.backend.user.dto.friend.FriendRequestDto;
import com.dondoc.backend.user.dto.friend.FriendSearchDto;
import com.dondoc.backend.user.entity.Account;
import com.dondoc.backend.user.entity.Friend;
import com.dondoc.backend.user.entity.User;
import com.dondoc.backend.user.repository.AccountRepository;
import com.dondoc.backend.user.repository.FriendRepository;
import com.dondoc.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class FriendServiceImpl implements FriendService{

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    private final AccountRepository accountRepository;

    public FriendServiceImpl(FriendRepository friendRepository, UserRepository userRepository, AccountRepository accountRepository) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    // 친구 요청 보내기
    @Override
    @Transactional
    public FriendRequestDto.Response friendRequest(Long friendId, Long userId) {
        if(friendId == userId){
            throw new NoSuchElementException("자신에게 요청을 보낼 수 없습니다.");
        }
        if(friendRepository.findByUserIdAndFriendId(userId, friendId).isPresent()){
            throw new NoSuchElementException("이미 존재한 요청 또는 친구입니다.");
        }
        if(friendRepository.findByFriendIdAndUserId(userId, friendId).isPresent()){
            throw new NoSuchElementException("이미 존재한 요청 또는 친구입니다.");
        }
        // 내 정보
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        // 보내는 요청
        Friend sendRequest = Friend.builder()
                .friendId(friendId)
                .user(me)
                .build();

        friendRepository.save(sendRequest);

        return FriendRequestDto.Response.builder()
                .msg("요청이 완료되었습니다.")
                .success(true)
                .build();
    }

    @Override
    @Transactional
    public FriendRequestDto.Response requestDelete(Long userId, Long requestId) {
        Friend friend = friendRepository.findByIdAndStatus(requestId, 0)
                .orElseThrow(() -> new NotFoundException("요청이 없습니다."));

        if(friend.getUser().getId() != userId){
            throw new NoSuchElementException("요청에 대한 접근 권한이 없습니다.");
        }

        // 삭제 수행
        friendRepository.delete(friend);

        return FriendRequestDto.Response.builder()
                .success(true)
                .msg("성공적으로 요청을 삭제했습니다.")
                .build();
    }

    // 친구 삭제
    @Override
    @Transactional
    public FriendRequestDto.Response friendDelete(Long userId, String id) {
        Long ID = Long.parseLong(id);
        Friend friend = friendRepository.findById(ID)
                .orElseThrow(() -> new NotFoundException("친구를 찾을 수 없습니다."));

        if(friend.getFriendId() != userId && friend.getUser().getId() != userId){
            throw new NoSuchElementException("삭제에 실패 했습니다.");
        }

        // 삭제 수행
        friendRepository.delete(friend);

        return FriendRequestDto.Response.builder()
                .msg("삭제를 완료하였습니다.")
                .success(true)
                .build();
    }

    // 친구 검색
    @Override
    public FriendSearchDto.Response searchFriend(Long userId, String phoneNumber) {
        // 나 검색
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        // 핸드폰 번호 기반으로 유저 탐색
        User friend = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        // 양방향 검색 해야함
        Friend result;
        try{
            // 친구 ID 기준으로 검색
            result = friendRepository.findByFriendIdAndUserId(friend.getId(), userId)
                    .orElseThrow(() -> new NotFoundException("친구를 찾을 수 없습니다."));

        }catch(Exception e){
            result = friendRepository.findByFriendIdAndUserId(userId, friend.getId())
                    .orElseThrow(() -> new NotFoundException("친구를 찾을 수 없습니다."));
        }

        FriendSearchDto.FriendInfo friendInfo;

        if(friend.getMainAccount() == null){
            friendInfo = FriendSearchDto.FriendInfo.builder()
                    .id(friend.getId())
                    .name(friend.getName())
                    .imageNumber(friend.getImageNumber() + "")
                    .phoneNumber(friend.getPhoneNumber())
                    .build();
        }else{
            Account account = accountRepository.findById(friend.getMainAccount())
                    .orElseThrow(() -> new NotFoundException("대표계좌가 없습니다."));

            friendInfo = FriendSearchDto.FriendInfo.builder()
                    .id(friend.getId())
                    .name(friend.getName())
                    .imageNumber(friend.getImageNumber() + "")
                    .phoneNumber(friend.getPhoneNumber())
                    .bankName(account.getBankName())
                    .bankCode(account.getBankCode())
                    .accountNumber(account.getAccountNumber())
                    .build();
        }

        return FriendSearchDto.Response.builder()
                .success(true)
                .msg("친구 검색에 성공하였습니다.")
                .friend(friendInfo)
                .build();
    }

    // 요청 승인
    @Override
    @Transactional
    public FriendRequestDto.Response requestAccess(Long userId, String requestId) {
        Friend friend = friendRepository.findByIdAndStatus(Long.parseLong(requestId), 0)
                .orElseThrow(() -> new NotFoundException("요청을 찾을 수 없습니다."));

        if(friend.getFriendId() != userId){
            throw new NoSuchElementException("요청에 접근 권한이 없습니다.");
        }

        // 상태 변경
        friend.setStatus(1);

        friendRepository.save(friend);

        return FriendRequestDto.Response.builder()
                .msg("수락이 완료되었습니다.")
                .success(true)
                .build();
    }

    // 요청 거절
    @Override
    @Transactional
    public FriendRequestDto.Response requestDeny(Long userId, String requestId) {
        Friend friend = friendRepository.findById(Long.parseLong(requestId))
                .orElseThrow(() -> new NotFoundException("요청을 찾을 수 없습니다."));

        if(friend.getFriendId() != userId){
            throw new NoSuchElementException("요청에 접근 권한이 없습니다.");
        }

        if(friend.getStatus() == 0){
            friendRepository.delete(friend);

            return FriendRequestDto.Response.builder()
                    .msg("거절이 완료 되었습니다.")
                    .success(true)
                    .build();
        }

        return FriendRequestDto.Response.builder()
                .msg("요청을 찾을 수 없습니다.")
                .success(false)
                .build();

    }

    // 받은 요청 목록
    @Override
    public FriendRequestDto.RequestListResponse receiveList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        List<Friend> list = friendRepository.findByFriendIdAndStatus(userId, 0);

        List<FriendRequestDto.ReceiveInfo> result = new ArrayList<>();

        for(Friend friend : list){
            FriendRequestDto.ReceiveInfo temp = FriendRequestDto.ReceiveInfo.builder()
                    .id(friend.getId())
                    .friendId(friend.getUser().getId())
                    .nickName(friend.getUser().getNickName())
                    .imageNumber(friend.getUser().getImageNumber())
                    .createdAt(friend.getCreatedAt())
                    .status(friend.getStatus())
                    .build();

            result.add(temp);
        }


        return FriendRequestDto.RequestListResponse.builder()
                .list(result)
                .msg("받은 요청 목록을 불러왔습니다.")
                .success(true)
                .build();
    }

    // 보낸 요청 목록
    @Override
    public FriendRequestDto.RequestListResponse sendList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        List<Friend> list = friendRepository.findByUserAndStatus(user, 0);

        List<FriendRequestDto.SendInfo> result = new ArrayList<>();

        for(Friend friend : list){
            User friendInfo = userRepository.findById(friend.getFriendId())
                    .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

            FriendRequestDto.SendInfo temp = FriendRequestDto.SendInfo.builder()
                    .id(friend.getId())
                    .userId(friendInfo.getId())
                    .nickName(friendInfo.getNickName())
                    .imageNumber(friendInfo.getImageNumber())
                    .createdAt(friend.getCreatedAt())
                    .status(friend.getStatus())
                    .build();

            result.add(temp);
        }

        return FriendRequestDto.RequestListResponse.builder()
                .list(result)
                .msg("보낸 요청 목록을 불러왔습니다.")
                .success(true)
                .build();
    }

    // 친구 목록
    @Override
    public FriendListDto.Response friendList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        // 조회한 친구 목록 객체
        List<Friend> list_receive = friendRepository.findByFriendIdAndStatus(userId, 1);
        List<Friend> list_send = friendRepository.findByUserAndStatus(user, 1);

        List<Friend> list = new ArrayList<>();

        for(Friend friend : list_receive){
            list.add(friend);
        }

        for(Friend friend : list_send){
            list.add(friend);
        }

        // 응답을 위한 친구 목록 객체
        List<FriendListDto.FriendInfoDto> friendList = new ArrayList<>();

        for(Friend friend : list){
            FriendListDto.FriendInfoDto temp;
            // 현재 이 정보가 나의 정보
            if(friend.getUser().getId().equals(userId)){
                temp = FriendListDto.FriendInfoDto.builder()
                        .id(friend.getId())
                        .userId(friend.getUser().getId())
                        .friendId(friend.getFriendId())
                        .createdAt(friend.getCreatedAt())
                        .build();
            }else{
                // 현재 이 정보가 상대의 정보
                temp = FriendListDto.FriendInfoDto.builder()
                        .id(friend.getId())
                        .userId(friend.getFriendId())
                        .friendId(friend.getUser().getId())
                        .createdAt(friend.getCreatedAt())
                        .build();
            }
            friendList.add(temp);
        }

        List<FriendListDto.FriendInfo> result = new ArrayList<>();

        for(FriendListDto.FriendInfoDto info : friendList){
            User search = userRepository.findById(info.getFriendId())
                    .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

            FriendListDto.FriendInfo friendInfo;
            if(search.getMainAccount() == null){
                friendInfo = FriendListDto.FriendInfo.builder()
                        .id(info.getId())
                        .userId(search.getId())
                        .name(search.getName())
                        .imageNumber(search.getImageNumber() + "")
                        .phoneNumber(search.getPhoneNumber())
                        .accountNumber("대표계좌가 없습니다.")
                        .build();
            }else{
                Account account = accountRepository.findByAccountId(search.getMainAccount())
                        .orElseThrow(() -> new NotFoundException("대표 계좌가 없습니다."));

                friendInfo = FriendListDto.FriendInfo.builder()
                        .id(info.getId())
                        .userId(search.getId())
                        .name(search.getName())
                        .imageNumber(search.getImageNumber() + "")
                        .phoneNumber(search.getPhoneNumber())
                        .bankName(account.getBankName())
                        .bankCode(account.getBankCode())
                        .accountNumber(account.getAccountNumber())
                        .build();
            }

            result.add(friendInfo);
        }

        return FriendListDto.Response.builder()
                .list(result)
                .msg("친구 목록을 불러왔습니다.")
                .success(true)
                .build();
    }

    //

}
