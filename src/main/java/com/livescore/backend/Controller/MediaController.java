package com.livescore.backend.Controller;

import com.livescore.backend.DTO.MediaDTo;
import com.livescore.backend.Entity.Media;
import com.livescore.backend.Service.MediaService;
import io.imagekit.sdk.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
@RestController

public class MediaController {
    @Autowired
    private MediaService mediaService;

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("matchId") Long matchId) throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {

        MediaDTo media = new MediaDTo();
        media.setMatchId(matchId);

        return mediaService.createMedia(file, media);
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<?> getMediaById(@PathVariable Long id) {
        return mediaService.getMediaByMatchId(id);
    }

    @GetMapping("/media")
    public ResponseEntity<?> getAllMedia() {
        return mediaService.getAllMedia();
    }

//
//    @DeleteMapping("/media/{id}")
//    public ResponseEntity<?> deleteMedia(@PathVariable Long id) {
//        return mediaService.deleteMedia(id);
//    }
//
//    //get media by match id(send files from path to api caller)
//    @GetMapping("media/match/{id}")
//    public ResponseEntity<?> getMediaByMatchId(@PathVariable Long id) {
//        return mediaService.getMediaByMatchId(id);
//    }
    @GetMapping("media/season/{id}")
    public ResponseEntity<?> getMediaBySeasonId(@PathVariable Long id) {
        return mediaService.getMediaBySeasonId(id);
    }
    @GetMapping("media/tournament/{id}")
    public ResponseEntity<?> getMediaByTournamentId(@PathVariable Long id) {
        return mediaService.getMediaByTournamentId(id);
    }
    //get by sport id
    @GetMapping("media/sport/{id}")
    public ResponseEntity<?> getMediaBySportId(@PathVariable Long id) {
        return mediaService.getMediaBySportId(id);
    }




}
