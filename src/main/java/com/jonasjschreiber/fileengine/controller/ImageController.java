package com.jonasjschreiber.fileengine.controller;

import com.jonasjschreiber.fileengine.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/images/")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Value("${app.absolute.upload.path:${user.home}/tmp}")
    public String uploadDir;

    @GetMapping("/list")
    public ResponseEntity getList() throws IOException {
        return new ResponseEntity<>(imageService.getList(), HttpStatus.OK);
    }

    @PostMapping("/uploadFile")
    public ResponseEntity uploadFile(@RequestParam("files") MultipartFile file, RedirectAttributes redirectAttributes) {
        imageService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/uploadFiles")
    public ResponseEntity uploadFiles(@RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes) {
        Arrays.asList(files)
            .stream()
            .forEach(file -> imageService.uploadFile(file));
        redirectAttributes.addFlashAttribute("message",
                MessageFormat.format("You successfully uploaded all files: %s",
                Arrays.stream(files).map(f -> f.getOriginalFilename()).collect(Collectors.toList())));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping(value = "/getThumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getThumbnail(@RequestParam("filename") String filename) throws IOException {
        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageService.getFile(uploadDir + "thumbs" + File.separator + filename));
    }

    @GetMapping(value = "/getImage", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImage(@RequestParam("filename") String filename) throws IOException {
        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageService.getFile(uploadDir + filename));
    }
}
