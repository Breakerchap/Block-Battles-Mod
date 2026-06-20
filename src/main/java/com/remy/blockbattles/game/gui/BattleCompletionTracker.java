package com.remy.blockbattles.game.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.remy.blockbattles.game.logic.BattleWarp;

public final class BattleCompletionTracker {
  private static final String RESOURCE_PATH = "/assets/blockbattles/encyclopedia/block_completionism.md";
  private static final Path DEV_PATH = Path.of("BLOCK_COMPLETIONISM.md");

  private static volatile CompletionData cachedData;

  private BattleCompletionTracker() {
  }

  public static BlockCompletionStatus getBlockStatus(String blockName) {
    return loadData().blockStatuses().getOrDefault(
        normalizeName(blockName),
        BlockCompletionStatus.missing());
  }

  public static WarpCompletionStatus getWarpStatus(BattleWarp warp) {
    return loadData().warpStatuses().getOrDefault(
        normalizeName(warp.getDisplayName()),
        WarpCompletionStatus.missing());
  }

  private static CompletionData loadData() {
    CompletionData localData = cachedData;

    if (localData != null) {
      return localData;
    }

    synchronized (BattleCompletionTracker.class) {
      if (cachedData == null) {
        cachedData = parse(readLines());
      }

      return cachedData;
    }
  }

  private static List<String> readLines() {
    try (InputStream resourceStream = BattleCompletionTracker.class.getResourceAsStream(RESOURCE_PATH)) {
      if (resourceStream != null) {
        return new ArrayList<>(new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
      }

      if (Files.exists(DEV_PATH)) {
        return Files.readAllLines(DEV_PATH, StandardCharsets.UTF_8);
      }

      return List.of();
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not load encyclopedia completion tracker.", exception);
    }
  }

  private static CompletionData parse(List<String> lines) {
    Map<String, BlockCompletionStatus> blockStatuses = new HashMap<>();
    Map<String, WarpCompletionStatus> warpStatuses = new HashMap<>();
    Section section = Section.NONE;

    for (String line : lines) {
      String trimmed = line.trim();

      if (trimmed.equals("## Blocks")) {
        section = Section.BLOCKS;
        continue;
      }

      if (trimmed.equals("## Warps")) {
        section = Section.WARPS;
        continue;
      }

      if (!trimmed.startsWith("|")) {
        continue;
      }

      List<String> cells = splitMarkdownRow(trimmed);

      if (cells.isEmpty() || cells.get(0).equalsIgnoreCase("Block") || cells.get(0).equalsIgnoreCase("Warp")
          || cells.get(0).startsWith("---")) {
        continue;
      }

      if (section == Section.BLOCKS && cells.size() >= 6) {
        blockStatuses.put(
            normalizeName(cells.get(0)),
            new BlockCompletionStatus(
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                cells.get(5)));
      } else if (section == Section.WARPS && cells.size() >= 5) {
        warpStatuses.put(
            normalizeName(cells.get(0)),
            new WarpCompletionStatus(
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4)));
      }
    }

    return new CompletionData(blockStatuses, warpStatuses);
  }

  private static List<String> splitMarkdownRow(String row) {
    String[] parts = row.split("\\|", -1);
    ArrayList<String> cells = new ArrayList<>();

    for (int index = 1; index < parts.length - 1; index++) {
      cells.add(parts[index].trim());
    }

    return cells;
  }

  private static String normalizeName(String name) {
    String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    normalized = normalized.replaceAll("\\s*\\([^)]*\\)$", "");
    normalized = normalized.replaceAll("`", "");
    normalized = normalized.replaceAll("\\s+", " ");
    return normalized;
  }

  private enum Section {
    NONE,
    BLOCKS,
    WARPS
  }

  private record CompletionData(
      Map<String, BlockCompletionStatus> blockStatuses,
      Map<String, WarpCompletionStatus> warpStatuses) {
  }

  public record BlockCompletionStatus(
      String implemented,
      String ability,
      String requirements,
      String combos,
      String notes) {
    public static BlockCompletionStatus missing() {
      return new BlockCompletionStatus("Unknown", "Unknown", "Unknown", "Unknown", "");
    }

    public String overallStatus() {
      return overallFromColumns(implemented, ability, requirements, combos);
    }
  }

  public record WarpCompletionStatus(
      String implemented,
      String effect,
      String combos,
      String notes) {
    public static WarpCompletionStatus missing() {
      return new WarpCompletionStatus("Unknown", "Unknown", "Unknown", "");
    }

    public String overallStatus() {
      return overallFromColumns(implemented, effect, combos);
    }
  }

  private static String overallFromColumns(String... values) {
    if (values.length > 0 && values[0].equalsIgnoreCase("No")) {
      return "Missing";
    }

    boolean hasPartial = false;
    boolean hasMissing = false;
    boolean hasDone = false;

    for (String rawValue : values) {
      String value = rawValue == null ? "" : rawValue.trim();

      if (value.equalsIgnoreCase("Partial")) {
        hasPartial = true;
      } else if (value.equalsIgnoreCase("Missing")) {
        hasMissing = true;
      } else if (value.equalsIgnoreCase("Done") || value.equalsIgnoreCase("Yes") || value.equalsIgnoreCase("N/A")) {
        hasDone = true;
      }
    }

    if (hasMissing || hasPartial) {
      return "Partial";
    }

    if (hasDone) {
      return "Complete";
    }

    return "Unknown";
  }
}
