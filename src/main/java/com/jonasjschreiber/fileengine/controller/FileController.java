package com.jonasjschreiber.fileengine.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.jonasjschreiber.fileengine.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/v1/")
public class FileController {

    @Autowired
    FileService fileService;

    @GetMapping
    public String index() {
        return "upload";
    }

    @GetMapping("/listFiles")
    public ResponseEntity getFilenames() throws IOException {
        return new ResponseEntity<>(fileService.getFileNames(), HttpStatus.OK);
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        fileService.uploadFile(file);
        redirectAttributes.addFlashAttribute("message",
            "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/v1/";
    }

    @PostMapping("/uploadFiles")
    public String uploadFiles(@RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes) {
        Arrays.asList(files)
            .stream()
            .forEach(file -> fileService.uploadFile(file));
        redirectAttributes.addFlashAttribute("message",
            "You successfully uploaded all files!");
        return "redirect:/v1/";
    }
}
