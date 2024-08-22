// Definimos los posibles tipos de nodos
type NodeType = string;

// Atributos comunes para los nodos
interface NodeAttributes {
    charCount?: number;
    wordCount?: number;
    readingTime?: number;
    language?: string | null;
}

// Definimos el tipo para el contenido de texto
export interface TextNode {
    type: 'text';
    text: string;
}

// Definimos el tipo para un nodo genérico
export interface GenericNode {
    type: NodeType;
    attrs?: NodeAttributes;
    content?: Array<GenericNode | TextNode>;
}

// Definimos el tipo para el nodo raíz que es de tipo 'doc'
interface DocumentNode extends GenericNode {
    type: 'doc';
    attrs: {
        charCount: number;
        wordCount: number;
        readingTime: number;
    };
    content: Array<GenericNode | TextNode>;
}

// Tipo principal que representa la estructura completa
export type RootNode = DocumentNode;
