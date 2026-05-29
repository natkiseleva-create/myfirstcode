#version 330 core
in vec4 vColor;
in vec3 vNormal;
in vec3 vFragPos;

uniform vec3 uSunDir = vec3(0.5, 0.7, 0.3);
uniform vec3 uSunColor = vec3(0.95, 0.9, 0.8);
uniform vec3 uAmbient = vec3(0.45, 0.45, 0.55);

out vec4 FragColor;

void main() {
    vec3 normal = normalize(vNormal);
    float diff = max(dot(normal, normalize(uSunDir)), 0.0);
    vec3 lighting = uAmbient + uSunColor * diff;
    FragColor = vec4(vColor.rgb * lighting, vColor.a);
}
