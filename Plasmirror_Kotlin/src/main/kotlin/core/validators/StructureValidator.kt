package core.validators

import core.*
import core.optics.Regime

/**
 * StructureValidator analyzes structure file as a String
 * and builds StructureDescription using LayerDescription and BlockDescription
 */
object StructureValidator {
  private var numberOfParameters = mapOf(
    "1-1" to 2, "1-2" to 2, "1-3" to 2,
    "2-1" to 4, "2-2" to 4, "2-3" to 4,
    "3" to 3,
    "4-1" to 5, "4-2" to 5, "4-3" to 5,
    "5-1" to 7, "5-2" to 7, "5-3" to 7,
    "6" to 6,
    "7-1-1" to 8, "7-2-1" to 8, "7-3-1" to 8,
    "7-1-2" to 5, "7-2-2" to 5, "7-3-2" to 5,
    "8-1-1" to 9, "8-2-1" to 9, "8-3-1" to 9,
    "8-1-2" to 6, "8-2-2" to 6, "8-3-2" to 6,
    "9-1-1" to 8, "9-2-1" to 8, "9-3-1" to 8,
    "9-1-2" to 5, "9-2-2" to 5, "9-3-2" to 5
  )

  /**
   * Validates and builds structure description representation from the string
   */
  fun initStructure(): ValidationResult {
    try {
      val lines = toLines(StructureDescriptionStorage.description)
      val tokenizedLines = linesToTokenizedLines(lines)
      validateTokenizedLines(tokenizedLines)
      val structureDescription = buildStructureDescription(tokenizedLines)
      State.structure = buildStructure(structureDescription)
    } catch (e: StructureDescriptionException) {
      alert(headerText = "Structure description error", contentText = e.message ?: "")
      return ValidationResult.FAILURE
    }
    return ValidationResult.SUCCESS
  }

  /**
   * Maps structure string representation to the list of lines
   * such that each line represents description of a layer or period.
   * Ignore: single-line comments, multi-line comments
   * Expand each layer description named tokens to single line:
   *
   *    type = 7-2, d = 10, x = 0.31,
   *    wPlasma = 7.38, gammaPlasma = 0.18,  ---->  type = 7-2, d = 10, x = 0.31, wPlasma = 7.38, gammaPlasma = 0.18, f = 0.0017
   *    f = 0.0017
   *
   * Remove empty lines
   *
   * (?s) activates Pattern.DOTALL notation.
   * "In dotall mode, the expression . matches any character, including a line terminator.
   * By default this expression does not match line terminators."
   *
   * @param structure structure string representation
   * @return structure representation as lines
   */
  @Throws(StructureDescriptionException::class)
  private fun toLines(structure: String): List<String> = structure.toLowerCase()
    /** exclude multi-line comments */
    .replace(Regex("(?s)/\\*.*\\*/"), "")
    /** exclude single-line comments */
    .replace(Regex("\\s*[/]{2,}.*"), "")
    /**
    replace all new line characters by whitespaces
    (the case when the structure description of some layer occupied several lines)
     */
    .replace(Regex("\\R*"), "")
    /** remove all whitespaces */
    .replace(Regex("\\s*"), "")
    /** put period descriptions to separate lines */
    .replace(Regex("([x][0-9]+)"), "\n\$1\n")
    /** each line will start with the keyword "type" */
    .replace(Regex("(type)"), "\n\$1")
    .lines()
    .map { it.trim() }
    .filter { it.isNotBlank() }

  /**
   * Tokenizes each line of the structure
   *
   * @param lines structure representation as lines
   * @return list of tokenized lines (each tokenized line is a list of tokens)
   */
  private fun linesToTokenizedLines(lines: List<String>) = mutableListOf<List<String>>().apply {
    /** remove names of layer parameters ("type=", "d=", "k=", "x=") */
    lines.forEach { add(it.split(",").map { it.replace(Regex(".+=+"), "") }) }
  }

  /**
   * Validates each tokenized line representing symbolic description of a layer
   *
   * @param tokenizedLines structure representation as lines
   */
  @Throws(StructureDescriptionException::class)
  private fun validateTokenizedLines(tokenizedLines: List<List<String>>) = with(tokenizedLines) {
    /**
    Structure description is empty
     */
    if (isEmpty()) {
      throw StructureDescriptionException("Empty structure description")
    }
    /**
    Structure description should start with period repeat number
     */
    if (this[0][0].startsWith("x").not()) {
      throw StructureDescriptionException("Structure description should start with period repeat description")
    }
    /**
    2 or more consecutive period descriptions
     */
    (1 until size).filter { this[it - 1][0].contains("x") && this[it][0].contains("x") }.forEach {
      throw StructureDescriptionException("Two period repeat descriptions are found together")
    }
    /**
    Check period description format
     */
    with(flatMap { it }.filter { it.contains(Regex("^[x][0-9]+$")) }) {
      /**
      There must be a period repeat description using format "x123"
       */
      if (isEmpty()) {
        throw StructureDescriptionException("Period repeat description does not match the specified format")
      }
      /**
      Each period repeat number should be parsed to Int
       */
      map { it.substring(1) }.forEach { repeat ->
        try {
          repeat.toInt()
        } catch (e: NumberFormatException) {
          throw StructureDescriptionException("Period repeat number error")
        }
      }
    }
    /**
    Check that structure contains at least one layer
     */
    with(flatMap { it }.filterNot { it.contains("x") }) {
      if (isEmpty()) {
        throw StructureDescriptionException("Structure must contain a single layer at least")
      }
    }
    /**
    Check that structure contains only one layer for
    PERMITTIVITY, REFRACTIVE_INDEX, EXTINCTION_COEFFICIENT and SCATTERING_COEFFICIENT regimes
    (check that tokenizedLines contains only one List<String> with layer parameters)
     */
    when (State.regime) {
      Regime.PERMITTIVITY,
      Regime.REFRACTIVE_INDEX,
      Regime.EXTINCTION_COEFFICIENT,
      Regime.SCATTERING_COEFFICIENT -> {
        if (filterNot { it.all { it.contains("x") } }.size != 1) {
          throw StructureDescriptionException("Structure must contain only one layer for this regime")
        }
      }
      else -> {
      }
    }

    /**
    Layer type check
     */
    filterNot { it[0].startsWith("x") }.forEach {
      val type = it[0]
      /**
      Check that layer type corresponds to a Mie theory layer for a light scattering computation
       */
      if (State.regime == Regime.SCATTERING_COEFFICIENT) {
        if (type.startsWith("8").not()) {
          throw StructureDescriptionException(
            "Scattering computation must be provided for a single layer " +
              "considered in terms of Mie theory"
          )
        }
      }
      /**
      Check the layer type parameter to correspond to the appropriate number of parameters for a layer
       */
      if (type !in numberOfParameters.keys || numberOfParameters[type] != it.size) {
        throw StructureDescriptionException("Invalid layer type or incorrect number of parameters for a layer")
      }
    }
    /**
    Check complex and double parameters format
     */
    with(flatMap { it }) {
      /**
      Complex parameter description should contain both "(" and ")"
       */
      forEach {
        // TODO check replacement by
        // (it.contains("(") && it.contains(")")).not()
        if ((it.contains("(") && it.contains(")").not()) || (it.contains("(").not() && it.contains(")"))) {
          throw StructureDescriptionException("Invalid complex parameter value format")
        }
      }
      /**
      Complex numbers must use the format of "(a; b)"
      and should be parsed to real: Double and imaginary: Double
       */
      filter { it.contains("(") && it.contains(")") }
        .map { it.replace(Regex("\\("), "") }
        .map { it.replace(Regex("\\)"), "") }
        .forEach {
          it.split(";").let {
            try {
              it[0].toDouble() // real
              it[1].toDouble() // imaginary
            } catch (e: NumberFormatException) {
              throw StructureDescriptionException("Complex number format error")
            }
          }
        }
      /**
      Check double parameters format. Each parameter value should be parsed to Double.
      Exclude complex, period and type
       */
      filterNot { it.contains("x") || it.contains("(") || it.contains("-") }.forEach { value ->
        try {
          value.toDouble()
        } catch (e: NumberFormatException) {
          throw StructureDescriptionException("Invalid parameter value format")
        }
      }
    }
  }

  /**
   * Builds the StructureDescription object using the tokenized data for layers
   *
   * @param tokenizedLines structure representation as lines
   * @return structure description object
   */
  // TODO kotlin style
  private fun buildStructureDescription(tokenizedLines: List<List<String>>) = StructureDescription().apply {
    val blockDescriptions = blockDescriptions
    var layerIndex = 0
    with(tokenizedLines) {
      while (layerIndex < size) {
        blockDescriptions += BlockDescription(repeat = this[layerIndex][0].substring(1))
        layerIndex++
        /* lazy evaluation. Need to check layerIndex < size FIRST. Otherwise IndexOutOfBoundsException!!! */
        while (layerIndex < size && this[layerIndex].isRepeat().not()) {
          /**
          Add LayerDescriptions to the last BlockDescription
           */
          blockDescriptions[blockDescriptions.size - 1].layerDescriptions +=
            LayerDescription(type = this[layerIndex][0],
              description = this[layerIndex].subList(1, this[layerIndex].size))
          layerIndex++
        }
      }
    }
  }

  /**
   * Builds structure object using the given structure description object
   *
   * @param structureDescription description object
   * @return structure object
   */
  private fun buildStructure(structureDescription: StructureDescription) =
    StructureBuilder.build(structureDescription)

  /**
   * Checks if the line: String contains only one token and represents a repeat number for a block
   */
  private fun List<String>.isRepeat() = size == 1 && this[0].startsWith("x")

  private class StructureDescriptionException(message: String) : RuntimeException(message)
}

