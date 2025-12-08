package com.livescore.backend.Service;

import com.livescore.backend.DTO.MediaDTo;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Media;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.MediaInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service

public class MediaService {
    @Autowired
    private MediaInterface mediaInterface;
    @Autowired
    private MatchInterface matchInterface;
    @Autowired
    private TournamentInterface tournamentInterface;

    public ResponseEntity<?> createMedia(MultipartFile f, MediaDTo media) throws IOException {

        if (f == null || f.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Media file is required")
            );
        }

        // Validate matchId
        if (media.getMatchId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Match ID is required")
            );
        }

        // Check if match exists
        Optional<Match> matchOpt = matchInterface.findById(media.getMatchId());
        if (matchOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Match not found with ID: " + media.getMatchId())
            );
        }

        // Create media entity
        Media mediaEntity = new Media();
        mediaEntity.setFileData(f.getBytes());
        mediaEntity.setFileType(f.getContentType()); // Use content type from file
        mediaEntity.setMatch(matchOpt.get());

        // Save and return
        Media savedMedia = mediaInterface.save(mediaEntity);

        return ResponseEntity.ok(Map.of(
                "message", "Media uploaded successfully",
                "mediaId", savedMedia.getId(),
                "fileType", savedMedia.getFileType(),
                "matchId", savedMedia.getMatch().getId()
        ));
    }

    public ResponseEntity<?> getMediaById(Long id) {
        return mediaInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAllMedia() {
        return ResponseEntity.ok(mediaInterface.findAll());
    }

    public ResponseEntity<?> updateMedia(Long id, MultipartFile f, MediaDTo media) throws IOException {
        return mediaInterface.findById(id).map(mediaEntity -> {
            try {
                mediaEntity.setFileData(f.getBytes());
                mediaEntity.setFileType(getFileExtension(f.getOriginalFilename()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ResponseEntity.ok(mediaInterface.save(mediaEntity));
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> deleteMedia(Long id) {
        if(mediaInterface.existsById(id)){
            mediaInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
