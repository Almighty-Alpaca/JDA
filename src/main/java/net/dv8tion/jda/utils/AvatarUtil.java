/**
 * Copyright 2015 Austin Keener & Michael Ritter
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.utils;

import org.apache.commons.codec.binary.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class AvatarUtil
{
    public static final Avatar DELETE_AVATAR = new Avatar(null);
    private static final int SIZE = 128;

    public static Avatar getAvatar(File avatarFile) throws UnsupportedEncodingException
    {
        String[] split = avatarFile.getName().split("\\.");
        String type = split[split.length - 1];
        if (type.equalsIgnoreCase("jpg") || type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("png"))
        {
            try
            {
                //reading
                BufferedImage img = ImageIO.read(avatarFile);
                //resizing
                img = resize(img);
                //writing + converting to jpg if necessary
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", bout);
                bout.close();

                return new Avatar("data:image/jpeg;base64," + StringUtils.newStringUtf8(Base64.getEncoder().encode(bout.toByteArray())));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            throw new UnsupportedEncodingException("Image type " + type + " is not supported!");
        }
        return null;
    }

    private static BufferedImage resize(BufferedImage originalImage){
        BufferedImage resizedImage = new BufferedImage(SIZE, SIZE,
                originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, SIZE, SIZE, Color.white, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        return resizedImage;
    }

    public static class Avatar
    {
        private final String encoded;

        private Avatar(String encoded)
        {
            this.encoded = encoded;
        }

        public String getEncoded()
        {
            return encoded;
        }
    }
}
