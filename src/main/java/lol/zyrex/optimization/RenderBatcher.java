package lol.zyrex.optimization;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Батчер для минимизации GL вызовов
 * Собирает примитивы и рендерит их одним draw call
 */
public class RenderBatcher {
    
    private static RenderBatcher instance;
    
    // Батчи для разных типов примитивов
    private final List<BatchedLine> linesBatch = new ArrayList<>(256);
    private final List<BatchedQuad> quadsBatch = new ArrayList<>(128);
    
    private boolean isBatching = false;
    private float currentLineWidth = 1.0f;
    
    public static RenderBatcher getInstance() {
        if (instance == null) {
            instance = new RenderBatcher();
        }
        return instance;
    }
    
    /**
     * Начать батчинг
     */
    public void beginBatch() {
        isBatching = true;
        linesBatch.clear();
        quadsBatch.clear();
    }
    
    /**
     * Добавить линию в батч
     */
    public void batchLine(MatrixStack.Entry entry, Vector3f start, Vector3f end, int colorStart, int colorEnd, float width) {
        if (!isBatching) return;
        linesBatch.add(new BatchedLine(entry, start, end, colorStart, colorEnd, width));
    }
    
    /**
     * Добавить квад в батч
     */
    public void batchQuad(MatrixStack.Entry entry, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int color) {
        if (!isBatching) return;
        quadsBatch.add(new BatchedQuad(entry, v1, v2, v3, v4, color));
    }
    
    /**
     * Завершить батчинг и отрендерить всё одним вызовом
     */
    public void endBatch() {
        if (!isBatching) return;
        isBatching = false;
        
        // Рендерим линии
        if (!linesBatch.isEmpty()) {
            renderLines();
        }
        
        // Рендерим квады
        if (!quadsBatch.isEmpty()) {
            renderQuads();
        }
    }
    
    private void renderLines() {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        
        // Группируем по ширине линии для минимизации state changes
        float lastWidth = -1;
        BufferBuilder buffer = null;
        
        for (BatchedLine line : linesBatch) {
            if (line.width != lastWidth) {
                // Завершаем предыдущий батч если был
                if (buffer != null) {
                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                }
                
                RenderSystem.lineWidth(line.width);
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                lastWidth = line.width;
            }
            
            if (buffer != null && line.entry != null) {
                Vector3f normal = getNormal(line.start, line.end);
                buffer.vertex(line.entry, line.start).color(line.colorStart).normal(line.entry, normal);
                buffer.vertex(line.entry, line.end).color(line.colorEnd).normal(line.entry, normal);
            }
        }
        
        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    private void renderQuads() {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        for (BatchedQuad quad : quadsBatch) {
            if (quad.entry != null) {
                buffer.vertex(quad.entry, quad.v1).color(quad.color);
                buffer.vertex(quad.entry, quad.v2).color(quad.color);
                buffer.vertex(quad.entry, quad.v3).color(quad.color);
                buffer.vertex(quad.entry, quad.v4).color(quad.color);
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    private Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float len = normal.length();
        if (len > 0) {
            normal.div(len);
        }
        return normal;
    }
    
    public boolean isBatching() {
        return isBatching;
    }
    
    // Внутренние классы для хранения данных
    private record BatchedLine(MatrixStack.Entry entry, Vector3f start, Vector3f end, int colorStart, int colorEnd, float width) {}
    private record BatchedQuad(MatrixStack.Entry entry, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int color) {}
}